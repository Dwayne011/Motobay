#!/usr/bin/env python3
"""Builds data/motorcycle-garages.geojson: motorcycle repair garages across the UK from
OpenStreetMap (shop=motorcycle_repair and craft=motorcycle_repair).

The base map already shows motorcycle dealers (shop=motorcycle) and car garages
(shop=car_repair), but its tiles leave motorcycle repair out, so the app adds these as
ordinary map places, styled exactly like the base map's own. Runs in the weekly workflow.

Usage: python3 scripts/fetch_garages.py data/motorcycle-garages.geojson
"""
import json
import sys

from fetch_uk_bays import ENDPOINTS, USER_AGENT  # noqa: F401 - same servers and identity
import urllib.parse
import urllib.request
import time

QUERY = """
[out:json][timeout:300];
area["ISO3166-1"="GB"][admin_level=2]->.uk;
(
  nwr["shop"="motorcycle_repair"](area.uk);
  nwr["craft"="motorcycle_repair"](area.uk);
);
out center tags;
"""


def fetch():
    body = urllib.parse.urlencode({"data": QUERY}).encode()
    errors = []
    for attempt in range(3):
        for url in ENDPOINTS:
            try:
                req = urllib.request.Request(url, data=body, headers={"User-Agent": USER_AGENT})
                with urllib.request.urlopen(req, timeout=400) as res:
                    return json.load(res)
            except Exception as e:  # noqa: BLE001 - try the next server
                errors.append(f"{url}: {e}")
        time.sleep(30 * (attempt + 1))
    raise SystemExit("All Overpass servers failed:\n" + "\n".join(errors))


def build(response):
    features = []
    for el in response.get("elements", []):
        lat = el.get("lat", (el.get("center") or {}).get("lat"))
        lon = el.get("lon", (el.get("center") or {}).get("lon"))
        if lat is None or lon is None:
            continue
        tags = el.get("tags") or {}
        name = tags.get("name") or tags.get("brand") or tags.get("operator")
        props = {"id": f"{el['type']}/{el['id']}"}
        if name:
            props["name"] = name
        features.append({
            "type": "Feature",
            "geometry": {"type": "Point", "coordinates": [round(lon, 6), round(lat, 6)]},
            "properties": props,
        })
    features.sort(key=lambda f: f["properties"]["id"])
    return {
        "type": "FeatureCollection",
        "attribution": "© OpenStreetMap contributors, ODbL 1.0",
        "features": features,
    }


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "data/motorcycle-garages.geojson"
    data = build(fetch())
    if len(data["features"]) < 50:
        raise SystemExit(f"Only {len(data['features'])} garages returned; refusing to publish.")
    try:
        with open(out, encoding="utf-8") as f:
            if json.load(f).get("features") == data["features"]:
                print(f"No changes ({len(data['features'])} garages).")
                return
    except (OSError, ValueError):
        pass
    with open(out, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    print(f"Wrote {len(data['features'])} garages to {out}")


if __name__ == "__main__":
    main()
