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
OUT = "data/map/graphite.json"

COLORS = dict(
    bg="#141414", water="#1b252d", park="#17211a", res="#171717", building="#202020",
    minor="#3c3c3c", casing="#141414", major="#5a5a5a", motorway="#7c7c7c", rail="#2e2e2e",
    label="#d4d4d4", halo="#141414",
)


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


if __name__ == "__main__":
    request = urllib.request.Request(SOURCE, headers={"User-Agent": "MotoBays style builder"})
    with urllib.request.urlopen(request, timeout=60) as res:
        base = json.load(res)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(recolor(base, COLORS), f, separators=(",", ":"))
    print(f"Wrote {OUT}")
