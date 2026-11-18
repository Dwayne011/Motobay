import unittest

from fetch_vehicle_crime import cell_of, cell_polygon, distance_m, in_scotland, summarise


def crime(cid, lat, lon):
    return {"id": cid, "location": {"latitude": str(lat), "longitude": str(lon)}}


class VehicleCrimeTest(unittest.TestCase):
    def test_scotland_by_postcode_then_latitude(self):
        self.assertTrue(in_scotland({"postcode": "EH1 1AA", "lat": 55.95, "lon": -3.19}))
        self.assertTrue(in_scotland({"postcode": "G2 1AA", "lat": 55.86, "lon": -4.25}))
        self.assertFalse(in_scotland({"postcode": "GU1 1AA", "lat": 51.24, "lon": -0.57}))  # Guildford, not Glasgow
        self.assertFalse(in_scotland({"postcode": "WC1B 3DG", "lat": 51.52, "lon": -0.12}))
        self.assertFalse(in_scotland({"lat": 51.5, "lon": -0.1}))
        self.assertTrue(in_scotland({"lat": 57.1, "lon": -2.1}))
        # In between, ask: the border runs between those latitudes.
        self.assertTrue(in_scotland({"lat": 55.0, "lon": -3.6}, lambda lat, lon: "Scotland"))
        self.assertFalse(in_scotland({"lat": 55.0, "lon": -1.6}, lambda lat, lon: "England"))
        self.assertFalse(in_scotland({"lat": 55.0, "lon": -1.6}, lambda lat, lon: None))

    def test_counts_within_200_m_and_dedupes(self):
        bays = [{"id": "a", "lat": 51.5200, "lon": -0.1200}, {"id": "s", "lat": 55.95, "lon": -3.19, "_scotland": True}]
        crimes = [
            crime(1, 51.5201, -0.1201),  # about 13 m away
            crime(1, 51.5201, -0.1201),  # same crime from an overlapping box
            crime(2, 51.5210, -0.1200),  # about 111 m
            crime(3, 51.5230, -0.1200),  # about 334 m: outside
        ]
        out = summarise(bays, crimes, ["2026-09", "2026-08", "2026-07"])
        self.assertEqual(out["bays"], {"a": 2})
        self.assertEqual(out["noData"], ["s"])
        self.assertEqual(out["radiusMetres"], 200)
        self.assertEqual(sum(p[2] for p in out["points"]), 3)
        self.assertEqual(out["months"], ["2026-09", "2026-08", "2026-07"])

    def test_typical_is_the_median_bay(self):
        bays = [{"id": str(i), "lat": 51.5 + i, "lon": 0.0} for i in range(5)]
        crimes = [crime(f"{i}-{k}", 51.5 + i, 0.0) for i in range(5) for k in range(i)]  # 0,1,2,3,4
        self.assertEqual(summarise(bays, crimes, [])["typical"], 2)

    def test_cell_box_covers_the_margin(self):
        bay = {"lat": 51.52049, "lon": -0.12351}
        corners = [tuple(map(float, p.split(","))) for p in cell_polygon(cell_of(bay)).split(":")]
        south, west = min(c[0] for c in corners), min(c[1] for c in corners)
        north, east = max(c[0] for c in corners), max(c[1] for c in corners)
        self.assertTrue(south < bay["lat"] < north and west < bay["lon"] < east)
        # Every bay in the cell has at least the 200 m radius inside the box.
        self.assertGreater(distance_m(bay["lat"], bay["lon"], south, bay["lon"]), 200)


if __name__ == "__main__":
    unittest.main()
