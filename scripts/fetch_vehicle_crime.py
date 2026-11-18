#!/usr/bin/env python3
"""
Builds data/vehicle-crime.json: police-recorded vehicle crime near each bay, from
data.police.uk (Open Government Licence v3.0), for the app's crime layer.

    python scripts/fetch_vehicle_crime.py data/uk-bays.json data/vehicle-crime.json

For the latest three months the police publish, it asks for "vehicle-crime" in a
small box around each group of bays, then:
  * counts, for every bay, the crimes recorded within RADIUS_M of it;
  * keeps each reported location and its count, for the heat layer.

The police place each crime at an anonymised "on or near" point, not where it
happened, so these are counts nearby, not a safety rating. Police Scotland doesn't
publish street-level data (only British Transport Police does there), so Scottish
bays are listed as having no data rather than a misleading zero.

Only reruns the download when the police have published a new month or the bays
have changed.
"""

import json
import math
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

USER_AGENT = "MotoBays data builder (https://github.com/Dwayne011/Motobay)"
API = "https://data.police.uk/api"
MONTHS = 3
RADIUS_M = 200
# Bays are grouped into cells of about 550 m; each cell's box is widened by MARGIN_M
# so bays near a cell edge still see crimes on the other side of it.
CELL_LAT = 0.005
CELL_LON = 0.008
MARGIN_M = 250
# data.police.uk allows 15 requests a second; stay well under it.
PAUSE_S = 0.12

SCOTTISH_POSTCODE = re.compile(r"^(AB|DD|DG|EH|FK|G|HS|IV|KA|KW|KY|ML|PA|PH|TD|ZE)\d", re.I)
# Without a postcode: all of Scotland is north of 54.63, and England stops at 55.81.
SCOTLAND_SOUTH = 54.63
ENGLAND_NORTH = 55.81


def distance_m(lat1, lon1, lat2, lon2):
    """Great-circle distance in metres."""
    r = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def in_scotland(bay, country_of=None):
    """Whether a bay is in Scotland: by postcode, else by latitude, else by asking [country_of]."""
    postcode = (bay.get("postcode") or "").strip()
    if postcode:
        return bool(SCOTTISH_POSTCODE.match(postcode))
    if bay["lat"] < SCOTLAND_SOUTH:
        return False
    if bay["lat"] > ENGLAND_NORTH:
        return True
    return country_of is not None and country_of(bay["lat"], bay["lon"]) == "Scotland"


def cell_of(bay):
    return (math.floor(bay["lat"] / CELL_LAT), math.floor(bay["lon"] / CELL_LON))


def cell_polygon(cell):
    """The cell widened by MARGIN_M, as data.police.uk's poly parameter (lat,lng:lat,lng:...)."""
    south, west = cell[0] * CELL_LAT, cell[1] * CELL_LON
    north, east = south + CELL_LAT, west + CELL_LON
    dlat = MARGIN_M / 111320
    dlon = MARGIN_M / (111320 * math.cos(math.radians((south + north) / 2)))
    s, n, w, e = south - dlat, north + dlat, west - dlon, east + dlon
    return ":".join(f"{lat:.5f},{lon:.5f}" for lat, lon in [(s, w), (n, w), (n, e), (s, e)])


def summarise(bays, crimes, months):
    """The output file from the bays and every crime fetched (deduplicated by id)."""
    unique = {}
    for c in crimes:
        unique[c["id"]] = c
    points = {}
    for c in unique.values():
        key = (round(float(c["location"]["latitude"]), 5), round(float(c["location"]["longitude"]), 5))
        points[key] = points.get(key, 0) + 1
    # Bucket the locations into a grid wider than RADIUS_M, so each bay checks only its 3 x 3 neighbourhood.
    step_lat, step_lon = 0.003, 0.005  # about 330 m each way in the UK
    grid = {}
    for (lat, lon), n in points.items():
        grid.setdefault((math.floor(lat / step_lat), math.floor(lon / step_lon)), []).append((lat, lon, n))
    counts = {}
    no_data = []
    for bay in bays:
        if bay.get("_scotland"):
            no_data.append(bay["id"])
            continue
        gy, gx = math.floor(bay["lat"] / step_lat), math.floor(bay["lon"] / step_lon)
        counts[bay["id"]] = sum(
            n
            for dy in (-1, 0, 1) for dx in (-1, 0, 1)
            for lat, lon, n in grid.get((gy + dy, gx + dx), ())
            if distance_m(bay["lat"], bay["lon"], lat, lon) <= RADIUS_M
        )
    ordered = sorted(counts.values())
    typical = ordered[len(ordered) // 2] if ordered else 0
    return {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "attribution": "Contains public sector information licensed under the Open Government Licence v3.0. Source: data.police.uk",
        "months": months,
        "radiusMetres": RADIUS_M,
        "typical": typical,
        "bays": counts,
        "noData": sorted(no_data),
        "points": [[lat, lon, n] for (lat, lon), n in sorted(points.items())],
    }


def get_json(url, tries=5):
    for attempt in range(tries):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=60) as res:
                return json.load(res)
        except urllib.error.HTTPError as e:
            if e.code not in (429, 500, 502, 503, 504) or attempt == tries - 1:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == tries - 1:
                raise
        time.sleep(2 ** attempt)
    raise RuntimeError("unreachable")


def postcodes_io_country(lat, lon):
    """The country of a point from postcodes.io's nearest postcode, or None."""
    try:
        data = get_json(f"https://api.postcodes.io/postcodes?lon={lon}&lat={lat}&limit=1&radius=2000")
        result = (data or {}).get("result") or []
        return result[0]["country"] if result else None
    except (urllib.error.URLError, KeyError, ValueError):
        return None


def latest_months():
    dates = get_json(f"{API}/crimes-street-dates")
    return sorted((d["date"] for d in dates), reverse=True)[:MONTHS]


def main(bays_path, out_path):
    bays = json.load(open(bays_path))["bays"]
    months = latest_months()
    try:
        previous = json.load(open(out_path))
    except (OSError, ValueError):
        previous = None
    known = set(previous["bays"]) | set(previous["noData"]) if previous else set()
    if previous and previous.get("months") == months and known == {b["id"] for b in bays}:
        print(f"Up to date for {months[-1]} to {months[0]}.")
        return

    for bay in bays:
        bay["_scotland"] = in_scotland(bay, postcodes_io_country)
    cells = sorted({cell_of(b) for b in bays if not b["_scotland"]})
    print(f"{len(cells)} areas x {len(months)} months ({months[-1]} to {months[0]})")
    crimes = []
    for i, cell in enumerate(cells, 1):
        poly = cell_polygon(cell)
        for month in months:
            url = f"{API}/crimes-street/vehicle-crime?" + urllib.parse.urlencode({"poly": poly, "date": month})
            crimes.extend(get_json(url) or [])
            time.sleep(PAUSE_S)
        if i % 100 == 0:
            print(f"  {i}/{len(cells)} areas, {len(crimes)} reports so far")
    out = summarise(bays, crimes, months)
    with open(out_path, "w") as f:
        json.dump(out, f, separators=(",", ":"))
        f.write("\n")
    print(f"Wrote {out_path}: {len(out['bays'])} bays, {len(out['noData'])} without data, "
          f"{len(out['points'])} locations, typical bay {out['typical']}.")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
