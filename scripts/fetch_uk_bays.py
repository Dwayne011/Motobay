#!/usr/bin/env python3
"""Builds data/uk-bays.json: every motorcycle parking location in the UK from
OpenStreetMap, normalised into the shape both apps read.

Runs weekly in GitHub Actions (.github/workflows/update-bay-data.yml), so the
apps download one file instead of every phone querying the shared Overpass
servers. Usage: python3 scripts/fetch_uk_bays.py data/uk-bays.json
"""
import json
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime, timezone

ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
]

QUERY = """
[out:json][timeout:600];
area["ISO3166-1"="GB"][admin_level=2]->.uk;
(
  nwr["amenity"="motorcycle_parking"](area.uk);
  nwr["amenity"="parking"]["motorcycle"="designated"](area.uk);
);
out center tags;
"""

USER_AGENT = "MotoBays data builder (https://github.com/Dwayne011/Motobay)"


def parse_fee(tags):
    fee = (tags.get("fee") or tags.get("fee:motorcycle") or "").lower()
    if fee == "no":
        return "free"
    if fee or "charge" in tags:
        return "paid"
    return "unknown"


def parse_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def to_bay(el):
    """Converts one Overpass element to a bay dict, or None if it has no position."""
    centre = el.get("center") or {}
    lat = el.get("lat", centre.get("lat"))
    lon = el.get("lon", centre.get("lon"))
    if lat is None or lon is None:
        return None
    tags = el.get("tags") or {}
    street = tags.get("addr:street")
    covered = tags.get("covered")
    bay = {
        "id": f"{el['type']}/{el['id']}",
        "lat": round(lat, 6),
        "lon": round(lon, 6),
        "name": tags.get("name") or (f"{street} motorcycle bay" if street else "Motorcycle parking"),
        "fee": parse_fee(tags),
        "covered": None if covered is None else covered in ("yes", "roof", "partial"),
        "capacity": parse_int(tags.get("capacity:motorcycle") or tags.get("capacity")),
        "openingHours": tags.get("opening_hours"),
        "maxStay": tags.get("maxstay"),
        "operator": tags.get("operator"),
        "street": street,
    }
    # Leave out empty fields to keep the download small.
    return {k: v for k, v in bay.items() if v is not None}


def fetch():
    body = urllib.parse.urlencode({"data": QUERY}).encode()
    errors = []
    for attempt in range(3):
        for url in ENDPOINTS:
            request = urllib.request.Request(url, data=body, headers={"User-Agent": USER_AGENT})
            try:
                with urllib.request.urlopen(request, timeout=900) as res:
                    return json.load(res)
            except Exception as e:  # noqa: BLE001 - try the next server
                errors.append(f"{url}: {e}")
                print(f"Failed {url}: {e}", file=sys.stderr)
        time.sleep(60 * (attempt + 1))
    raise SystemExit("All Overpass servers failed:\n" + "\n".join(errors))


def build(response):
    bays = [b for b in (to_bay(el) for el in response.get("elements", [])) if b]
    bays.sort(key=lambda b: b["id"])
    return {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "attribution": "© OpenStreetMap contributors, ODbL 1.0",
        "count": len(bays),
        "bays": bays,
    }


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "data/uk-bays.json"
    data = build(fetch())
    # Guard against a bad response wiping the published file.
    if data["count"] < 1000:
        raise SystemExit(f"Only {data['count']} bays returned; refusing to publish.")
    # Keep the old timestamp when nothing changed, so the weekly job doesn't
    # commit an identical file.
    try:
        with open(out, encoding="utf-8") as f:
            previous = json.load(f)
        if previous.get("bays") == data["bays"]:
            print(f"No changes ({data['count']} bays).")
            return
    except (OSError, ValueError):
        pass
    with open(out, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    print(f"Wrote {data['count']} bays to {out}")


if __name__ == "__main__":
    main()
