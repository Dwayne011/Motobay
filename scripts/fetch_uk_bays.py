#!/usr/bin/env python3
"""Builds data/uk-bays.json: every motorcycle parking location in the UK from
OpenStreetMap, normalised into the shape both apps read.

Runs weekly in GitHub Actions (.github/workflows/update-bay-data.yml), so the
apps download one file instead of every phone querying the shared Overpass
servers. Bays from council traffic orders (data/council-bays.json) are merged
in on top. Usage: python3 scripts/fetch_uk_bays.py data/uk-bays.json

To re-merge council bays into the existing file without refetching:
python3 scripts/fetch_uk_bays.py --merge-only data/uk-bays.json
"""
import os
import json
import math
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
  nwr["amenity"="parking_space"]["parking_space"="motorcycle"](area.uk);
  nwr["amenity"="parking_space"]["motorcycle"="designated"](area.uk);
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


def metres(a, b):
    """Approximate distance in metres; fine for the short distances used here."""
    dlat = (a["lat"] - b["lat"]) * 111_320
    dlon = (a["lon"] - b["lon"]) * 111_320 * math.cos(math.radians(a["lat"]))
    return math.hypot(dlat, dlon)


SPACE_MERGE_METRES = 25


def merge_spaces(bays, spaces):
    """Individual marked spaces become one bay per group of neighbouring spaces,
    with the group size as its capacity. Groups next to an existing bay are dropped
    as duplicates of it."""
    groups = []
    for space in spaces:
        for group in groups:
            if any(metres(space, other) <= SPACE_MERGE_METRES for other in group):
                group.append(space)
                break
        else:
            groups.append([space])
    merged = []
    for group in groups:
        lat = sum(s["lat"] for s in group) / len(group)
        lon = sum(s["lon"] for s in group) / len(group)
        centre = {"lat": lat, "lon": lon}
        if any(metres(centre, b) <= SPACE_MERGE_METRES for b in bays):
            continue
        first = dict(group[0])
        first["lat"], first["lon"] = round(lat, 6), round(lon, 6)
        first["capacity"] = sum(s.get("capacity", 1) for s in group)
        merged.append(first)
    return merged


OSM_ATTRIBUTION = "© OpenStreetMap contributors, ODbL 1.0"

# What a council bay carries into the published file. "approximate" marks a
# position placed from a nearby address rather than the exact reference point.
COUNCIL_FIELDS = ("id", "lat", "lon", "street", "openingHours", "maxStay", "lengthMetres", "note", "approximate")


def council_bays(council, osm_bays):
    """Bays from council traffic orders, minus any OpenStreetMap already has
    (within SPACE_MERGE_METRES). They go in as free, marked unconfirmed until the
    council confirms it, and with no capacity, which councils don't record."""
    sources = council.get("sources", {})
    out = []
    for c in council.get("bays", []):
        if any(metres(c, b) <= SPACE_MERGE_METRES for b in osm_bays):
            continue
        # A source is a plain name, or {"name": ..., "feeConfirmed": true} once the council confirms it's free.
        src = sources.get(c["source"], c["source"])
        if isinstance(src, str):
            src = {"name": src}
        bay = {k: c[k] for k in COUNCIL_FIELDS if k in c}
        bay["lat"], bay["lon"] = round(c["lat"], 6), round(c["lon"], 6)
        bay["name"] = "Motorcycle parking"
        bay["fee"] = "free"
        if not src.get("feeConfirmed"):
            bay["feeUnconfirmed"] = True
        bay["source"] = src["name"]
        out.append(bay)
    return out


def with_council(osm_bays, council):
    """The published file: OpenStreetMap bays plus council bays, sorted by id."""
    extra = council_bays(council, osm_bays) if council else []
    bays = sorted(osm_bays + extra, key=lambda b: b["id"])
    attribution = OSM_ATTRIBUTION
    if extra:
        attribution += "; " + "; ".join(sorted(set(b["source"] for b in extra)))
    return {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "attribution": attribution,
        "count": len(bays),
        "bays": bays,
    }


def build(response, council=None):
    bays, spaces = [], []
    for el in response.get("elements", []):
        bay = to_bay(el)
        if not bay:
            continue
        is_space = (el.get("tags") or {}).get("amenity") == "parking_space"
        (spaces if is_space else bays).append(bay)
    bays += merge_spaces(bays, spaces)
    return with_council(bays, council)


def load_council(out):
    path = os.path.join(os.path.dirname(os.path.abspath(out)), "council-bays.json")
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except OSError:
        return None


def main():
    args = [a for a in sys.argv[1:] if a != "--merge-only"]
    out = args[0] if args else "data/uk-bays.json"
    council = load_council(out)
    if "--merge-only" in sys.argv:
        with open(out, encoding="utf-8") as f:
            osm = [b for b in json.load(f)["bays"] if not b["id"].startswith("council/")]
        data = with_council(osm, council)
    else:
        data = build(fetch(), council)
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
