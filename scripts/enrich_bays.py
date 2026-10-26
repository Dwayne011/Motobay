#!/usr/bin/env python3
"""Adds a street, neighbourhood and outward postcode to each bay in
data/uk-bays.json by reverse geocoding with Photon (OpenStreetMap data), so
the apps can show "Wentworth Street, Spitalfields E1" instead of
"Motorcycle parking".

Results are cached in data/geocode-cache.json, keyed by bay id and position,
so the weekly job only looks up bays that are new or have moved.

Usage: python3 scripts/enrich_bays.py data/uk-bays.json [data/geocode-cache.json]
"""
import json
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

PHOTON = "https://photon.komoot.io/reverse"
USER_AGENT = "MotoBays data builder (https://github.com/Dwayne011/Motobay)"
PAUSE_SECONDS = 0.25

# Names OpenStreetMap mappers give bays that say nothing about where they are.
GENERIC_NAMES = {"motorcycle parking", "motorcycle bay", "motorcycle bays", "solo m/c bay", "solo motorcycle bay", "motorbike parking"}


def cache_key(bay):
    return f"{bay['id']}@{bay['lat']:.5f},{bay['lon']:.5f}"


def parse_place(response):
    """Pulls street, area and outward postcode from a Photon reverse response."""
    features = response.get("features") or []
    if not features:
        return {}
    p = features[0].get("properties") or {}
    street = p.get("street")
    if not street and p.get("osm_key") == "highway":
        street = p.get("name")
    area = p.get("locality") or p.get("district") or p.get("city") or p.get("town") or p.get("village") or p.get("county")
    city = p.get("city")
    postcode = (p.get("postcode") or "").split(" ")[0] or None
    place = {"street": street, "area": area, "postcode": postcode}
    # "Spitalfields" alone is ambiguous outside London; keep the town when it differs.
    if city and area and city != area:
        place["city"] = city
    return {k: v for k, v in place.items() if v}


def lookup(lat, lon):
    url = f"{PHOTON}?{urllib.parse.urlencode({'lat': lat, 'lon': lon, 'radius': 0.2})}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=30) as res:
                return parse_place(json.load(res))
        except Exception as e:  # noqa: BLE001
            print(f"Lookup {lat},{lon} failed ({e}); retrying", file=sys.stderr)
            time.sleep(5 * (attempt + 1))
    return None


LONDON_BOROUGH = re.compile(r"^(?:London Borough of|Royal Borough of|City of) (.+)$")
LONDON_POSTCODE = re.compile(r"^(?:E|EC|N|NW|SE|SW|W|WC)\d")


def tidy_place(place):
    """Normalises London places: "London Borough of Lambeth" becomes the area
    "Lambeth" in the city "London", and the town is dropped when it just repeats
    the area. Applied after the cache, so old entries are tidied too."""
    place = dict(place)
    london = False
    m = LONDON_BOROUGH.match(place.get("area") or "")
    if m:
        london = True
        place["area"] = m.group(1)
    if LONDON_BOROUGH.match(place.get("city") or ""):
        london = True
        place["city"] = "London"
    if LONDON_POSTCODE.match(place.get("postcode") or ""):
        london = True
    if london and not place.get("city"):
        place["city"] = "London"
    if place.get("city") and place["city"] == place.get("area"):
        del place["city"]
    return place


def apply(bay, place):
    """Merges a geocoded place into a bay. The street never overrides what OSM
    says; area, town and postcode only ever come from geocoding, so they are
    refreshed every run."""
    place = tidy_place(place)
    if place.get("street") and not bay.get("street"):
        bay["street"] = place["street"]
    for field in ("area", "city", "postcode"):
        bay.pop(field, None)
        if place.get(field):
            bay[field] = place[field]
    return bay


def enrich(bays, cache, fetch=lookup, pause=PAUSE_SECONDS, log=print):
    """Fills in street/area/postcode for every bay, using and extending [cache]."""
    missing = [b for b in bays if cache_key(b) not in cache]
    log(f"{len(bays)} bays, {len(missing)} to geocode")
    for i, bay in enumerate(missing, 1):
        place = fetch(bay["lat"], bay["lon"])
        if place is None:
            continue  # leave it out of the cache so the next run retries
        cache[cache_key(bay)] = place
        if i % 100 == 0:
            log(f"  {i}/{len(missing)}")
        time.sleep(pause)
    for bay in bays:
        place = cache.get(cache_key(bay))
        if place:
            apply(bay, place)
    return bays


def main():
    data_path = sys.argv[1] if len(sys.argv) > 1 else "data/uk-bays.json"
    cache_path = sys.argv[2] if len(sys.argv) > 2 else "data/geocode-cache.json"
    with open(data_path, encoding="utf-8") as f:
        data = json.load(f)
    try:
        with open(cache_path, encoding="utf-8") as f:
            cache = json.load(f)
    except (OSError, ValueError):
        cache = {}
    try:
        enrich(data["bays"], cache)
    finally:
        # Save progress even if interrupted, so a rerun picks up where it left off.
        with open(cache_path, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, sort_keys=True, indent=0)
    # fetch_uk_bays.py compares raw OSM bays against the enriched committed file,
    # so it always sees a change. Compare the enriched result against what is
    # committed instead, and keep the old timestamp when the bays are the same,
    # so a quiet week doesn't commit a timestamp-only change.
    try:
        committed = json.loads(subprocess.run(
            ["git", "show", f"HEAD:{data_path}"], capture_output=True, check=True, text=True,
        ).stdout)
        if committed.get("bays") == data["bays"]:
            data["generated"] = committed["generated"]
            print("Bays unchanged since the last commit.")
    except (subprocess.CalledProcessError, ValueError, KeyError, OSError):
        pass
    with open(data_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    done = sum(1 for b in data["bays"] if b.get("street"))
    print(f"{done}/{len(data['bays'])} bays now have a street")


if __name__ == "__main__":
    main()
