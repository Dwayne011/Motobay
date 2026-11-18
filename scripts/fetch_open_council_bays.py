#!/usr/bin/env python3
"""Builds data/open-council-bays.json: motorcycle bays that councils publish as open data
under the Open Government Licence, in the same shape as data/council-bays.json, which
fetch_uk_bays.py merges into the published bay file.

Runs in the weekly workflow before fetch_uk_bays.py. If a council's service is down,
that council's bays from the previous file are kept, so a bad week never removes them.

Sources (each checked for an explicit Open Government Licence statement):
- City of Edinburgh Council: Parking Bays layer, bay type "Motorcycle Parking Only".
- Leeds City Council: confirmed static traffic orders (Data Mill North), class "Motorcycles",
  only orders still in force.
- Nottingham City Council: Motorcycle Bays layer (a subset of its traffic orders).

No price is published, so every bay's price is "unknown".

Usage: python3 scripts/fetch_open_council_bays.py data/open-council-bays.json
"""
import io
import json
import os
import re
import sys
import tempfile
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone

from bng import bng_to_wgs84

USER_AGENT = "MotoBays data builder (https://github.com/Dwayne011/Motobay)"
YEAR = datetime.now(timezone.utc).year

SOURCES = {
    "edinburgh": {
        "name": "City of Edinburgh Council Parking Bays (Open Government Licence v3.0). "
                f"Copyright City of Edinburgh Council, contains Ordnance Survey data © Crown copyright and database right {YEAR}",
        "url": "https://data.edinburghcouncilmaps.info/",
    },
    "leeds": {
        "name": "Leeds City Council traffic orders, Data Mill North (Open Government Licence v3.0)",
        "url": "https://datamillnorth.org/dataset/29r8o",
    },
    "nottingham": {
        "name": "Nottingham City Council Motorcycle Bays (Open Government Licence v3.0). "
                f"Contains OS data © Crown copyright and database right {YEAR}",
        "url": "https://services.arcgis.com/yvqphKcf9bBSnjX1/arcgis/rest/services/Motorcycle_Bays/FeatureServer",
    },
}

EDINBURGH = "https://edinburghcouncilmaps.info/arcgis/rest/services/Misc/INSPIRE/MapServer/11/query"
NOTTINGHAM = "https://services.arcgis.com/yvqphKcf9bBSnjX1/arcgis/rest/services/Motorcycle_Bays/FeatureServer/76/query"
LEEDS_ZIP = ("https://datamillnorth.org/download/29r8o/d47359a5-a918-4618-95a8-275aeee0587f/"
             "New%20Compressed%20(zipped)%20Folder.zip")
LEEDS_LAYER = "All Confirmed Static Orders_lines"


def get(url, params=None, timeout=180):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=timeout) as res:
        return res.read()


def points(coords):
    """Every [x, y] in a GeoJSON coordinates array, however deeply nested."""
    if coords and isinstance(coords[0], (int, float)):
        return [coords]
    return [p for c in coords for p in points(c)]


def centre(pts):
    return sum(p[1] for p in pts) / len(pts), sum(p[0] for p in pts) / len(pts)


def tidy_street(name):
    """'THORESBY PLACE, CITY CENTRE' -> 'Thoresby Place'."""
    street = name.split(",")[0].strip()
    street = re.sub(r"\s*\(.*?\)\s*", " ", street).strip()
    return " ".join(w if w.isupper() and len(w) <= 2 else w.capitalize() for w in street.lower().split()) if street.isupper() else street


def sentence(*parts):
    """Joins a traffic order's pieces into one sentence: 'North side, from a point ..., for 6 metres.'"""
    kept = [re.sub(r"\s+", " ", p).strip().rstrip(".") for p in parts if p and p.strip()]
    kept = [k if i == 0 or not re.match(r"(The|From|For|To|A|An)\b", k) else k[0].lower() + k[1:] for i, k in enumerate(kept)]
    text = ", ".join(kept)
    return text[:1].upper() + text[1:] + "." if text else None


def stated_length(*texts):
    """The bay's length as the order states it ('for a distance of 6 metres'), or None."""
    for t in texts:
        m = re.search(r"distance of ([\d.]+) metres", t or "")
        if m:
            return float(m.group(1))
    return None


def arcgis_geojson(url, where):
    data = json.loads(get(url, {"where": where, "outFields": "*", "outSR": 4326, "f": "geojson"}))
    if "features" not in data:
        raise RuntimeError(f"Unexpected answer from {url}: {str(data)[:200]}")
    return data["features"]


def edinburgh():
    bays = []
    for f in arcgis_geojson(EDINBURGH, "Bay_Type='Motorcycle Parking Only'"):
        p, geom = f["properties"], f.get("geometry")
        if not geom or p.get("id") is None:
            continue
        lat, lon = centre(points(geom["coordinates"]))
        zone = (p.get("Zone_No") or "").strip()
        bays.append({
            "id": f"council/edinburgh/{int(p['id'])}",
            "source": "edinburgh",
            "lat": lat, "lon": lon,
            "fee": "unknown",
            "note": f"Motorcycle parking only. Controlled parking zone {zone}." if zone else "Motorcycle parking only.",
        })
    return bays


def nottingham():
    bays = []
    for f in arcgis_geojson(NOTTINGHAM, "1=1"):
        p, geom = f["properties"], f.get("geometry")
        if not geom:
            continue
        lat, lon = centre(points(geom["coordinates"]))
        street = re.split(r"\s{2,}", (p.get("Street_Name") or "").strip())[0]
        length = stated_length(p.get("End_Location"), p.get("Start_Location"))
        side = (p.get("Side_of_Road") or "").strip()
        ref = re.sub(r"[^a-z0-9]+", "-", f"{p.get('Order_Ref', '')} {street}".lower()).strip("-")
        bay = {
            "id": f"council/nottingham/{ref}",
            "source": "nottingham",
            "lat": lat, "lon": lon,
            "fee": "unknown",
            "street": street,
            "note": sentence(f"{side} side of {p.get('Street_Name', '').strip()}" if side else p.get("Street_Name"),
                             p.get("Start_Location"), p.get("End_Location"))
                    + (f" {p['Restriction'].strip().capitalize()}." if p.get("Restriction") else ""),
        }
        if length:
            bay["lengthMetres"] = length
        bays.append(bay)
    return bays


def leeds():
    import shapefile  # pyshp

    with tempfile.TemporaryDirectory() as tmp:
        zipfile.ZipFile(io.BytesIO(get(LEEDS_ZIP, timeout=600))).extractall(tmp)
        reader = shapefile.Reader(os.path.join(tmp, LEEDS_LAYER))
        fields = [f[0] for f in reader.fields[1:]]
        bays = []
        for sr in reader.iterShapeRecords():
            r = dict(zip(fields, sr.record))
            # Motorcycle-only bays, in force (no end date).
            if r.get("bClasses_o") != "Motorcycles" or (r.get("Date_to") or "").strip():
                continue
            pts = sr.shape.points
            if not pts:
                continue
            e = sum(p[0] for p in pts) / len(pts)
            n = sum(p[1] for p in pts) / len(pts)
            lat, lon = bng_to_wgs84(e, n)
            street = tidy_street(r.get("Street_nam") or "")
            side = (r.get("Side_of_Ro") or "").strip()
            where = sentence(f"{side} side" if side and side != "both" else ("Both sides" if side == "both" else ""),
                             r.get("start") or r.get("location"), r.get("finish"))
            bay = {
                "id": f"council/leeds/{int(r['item_ref'])}",
                "source": "leeds",
                "lat": lat, "lon": lon,
                "fee": "unknown",
                "street": street,
                "note": where,
            }
            length = stated_length(r.get("finish"), r.get("start"), r.get("location"))
            if length:
                bay["lengthMetres"] = length
            stay = re.sub(r"\s+", " ", r.get("bmax_perio") or "").strip()
            if stay:
                bay["maxStay"] = stay.lower()
            bays.append(bay)
        return bays


FETCHERS = {"edinburgh": edinburgh, "leeds": leeds, "nottingham": nottingham}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "data/open-council-bays.json"
    try:
        with open(out, encoding="utf-8") as f:
            previous = json.load(f)
    except (OSError, ValueError):
        previous = {"bays": []}
    bays, failed = [], []
    for key, fetch in FETCHERS.items():
        try:
            got = fetch()
            if not got:
                raise RuntimeError("no bays returned")
            print(f"{key}: {len(got)} bays")
            bays += got
        except Exception as e:  # noqa: BLE001 - keep last week's bays for this council
            kept = [b for b in previous.get("bays", []) if b.get("source") == key]
            print(f"{key}: failed ({e}); keeping {len(kept)} bays from the previous file", file=sys.stderr)
            failed.append(key)
            bays += kept
    for b in bays:
        b["lat"], b["lon"] = round(b["lat"], 6), round(b["lon"], 6)
    bays.sort(key=lambda b: b["id"])
    data = {
        "about": "Motorcycle bays from councils' open data under the Open Government Licence v3.0, built by "
                 "scripts/fetch_open_council_bays.py and merged into uk-bays.json like council-bays.json. "
                 "No council here publishes prices, so fee is 'unknown'.",
        "sources": {k: {"name": v["name"], "url": v["url"]} for k, v in SOURCES.items()},
        "bays": bays,
    }
    if previous.get("bays") == bays:
        print(f"No changes ({len(bays)} bays).")
        return
    with open(out, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Wrote {len(bays)} bays to {out}" + (f" (kept previous for {', '.join(failed)})" if failed else ""))


if __name__ == "__main__":
    main()
