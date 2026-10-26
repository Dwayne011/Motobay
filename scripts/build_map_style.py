#!/usr/bin/env python3
"""Builds data/map/graphite.json: the app's dark map style.

It is OpenFreeMap's "dark" style (OpenStreetMap data, no API key) with the
roads, water and parks lightened so they read clearly on a phone at night.
Re-run when OpenFreeMap updates their style:
    python3 scripts/build_map_style.py
"""
import copy
import json
import urllib.request

SOURCE = "https://tiles.openfreemap.org/styles/dark"
# Liberty has the shop/landmark/transit layers that Dark leaves out.
POI_SOURCE = "https://tiles.openfreemap.org/styles/liberty"
OUT = "data/map/graphite.json"

COLORS = dict(
    bg="#141414", water="#1b252d", park="#17211a", res="#171717", building="#202020",
    minor="#3c3c3c", casing="#141414", major="#5a5a5a", motorway="#7c7c7c", rail="#2e2e2e",
    label="#d4d4d4", halo="#141414",
    poi_label="#c8c8c8", transit_label="#9ec1e8",
)
POI_LAYERS = ("poi_r1", "poi_r7", "poi_r20", "poi_transit", "airport")


def recolor(style, c):
    s = copy.deepcopy(style)
    s["name"] = "MotoBays Graphite"
    for layer in s["layers"]:
        i = layer["id"]
        p = layer.setdefault("paint", {})
        if i == "background":
            p["background-color"] = c["bg"]
        elif i == "water":
            p["fill-color"] = c["water"]
        elif i == "waterway":
            p["line-color"] = c["water"]
        elif i in ("landcover_wood", "landuse_park"):
            p["fill-color"] = c["park"]
        elif i == "landuse_residential":
            p["fill-color"] = c["res"]
        elif i == "building":
            p["fill-color"] = c["building"]
        elif i in ("highway_minor", "highway_path", "road_pier"):
            p["line-color"] = c["minor"]
            p["line-opacity"] = 1
        elif i in ("highway_major_casing", "highway_motorway_casing"):
            p["line-color"] = c["casing"]
        elif i in ("highway_major_inner", "highway_major_subtle"):
            p["line-color"] = c["major"]
        elif i in ("highway_motorway_inner", "highway_motorway_subtle"):
            p["line-color"] = c["motorway"]
        elif i.startswith("railway"):
            p["line-color"] = c["bg"] if "dash" in i else c["rail"]
        if layer["type"] == "symbol":
            p["text-color"] = c["label"]
            p["text-halo-color"] = c["halo"]
    return s


def add_pois(style, liberty, c):
    """Copies Liberty's POI layers (shops, landmarks, stations) into the dark
    style. The sprite icons carry their own white discs, so only the label
    colours need changing; icons are drawn a little larger for the dark map."""
    by_id = {l["id"]: l for l in liberty["layers"]}
    for pid in POI_LAYERS:
        if pid not in by_id:
            continue
        sym = copy.deepcopy(by_id[pid])
        transit = pid == "poi_transit"
        paint = sym.setdefault("paint", {})
        paint["text-color"] = c["transit_label" if transit else "poi_label"]
        paint["text-halo-color"] = c["halo"]
        paint["text-halo-width"] = 1.2
        if "icon-image" in sym.get("layout", {}):
            # Transit badges are blue like the brand, so keep them a touch smaller
            # and softer than other places; bay pins must win.
            sym["layout"]["icon-size"] = 0.95 if transit else 1.15
            if transit:
                paint["icon-opacity"] = 0.85
        style["layers"].append(sym)
    return style


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "MotoBays style builder"})
    with urllib.request.urlopen(request, timeout=60) as res:
        return json.load(res)


if __name__ == "__main__":
    style = add_pois(recolor(fetch(SOURCE), COLORS), fetch(POI_SOURCE), COLORS)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(style, f, separators=(",", ":"))
    print(f"Wrote {OUT} with {len(style['layers'])} layers")
