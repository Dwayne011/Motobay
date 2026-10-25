import unittest

from fetch_uk_bays import build, to_bay


class ToBayTest(unittest.TestCase):
    def test_free_node(self):
        bay = to_bay({"type": "node", "id": 101, "lat": 51.51, "lon": -0.13,
                      "tags": {"amenity": "motorcycle_parking", "fee": "no", "capacity": "12", "covered": "no"}})
        self.assertEqual(bay, {"id": "node/101", "lat": 51.51, "lon": -0.13, "name": "Motorcycle parking",
                               "fee": "free", "covered": False, "capacity": 12})

    def test_paid_way_uses_centre(self):
        bay = to_bay({"type": "way", "id": 202, "center": {"lat": 51.52, "lon": -0.14},
                      "tags": {"fee": "yes", "name": "Soho Bike Bay", "maxstay": "4 hours", "covered": "roof"}})
        self.assertEqual(bay["id"], "way/202")
        self.assertEqual(bay["lat"], 51.52)
        self.assertEqual(bay["fee"], "paid")
        self.assertEqual(bay["maxStay"], "4 hours")
        self.assertTrue(bay["covered"])

    def test_car_park_with_motorcycle_spaces(self):
        bay = to_bay({"type": "node", "id": 303, "lat": 51.53, "lon": -0.15,
                      "tags": {"amenity": "parking", "motorcycle": "designated",
                               "addr:street": "Brewer Street", "capacity:motorcycle": "6"}})
        self.assertEqual(bay["fee"], "unknown")
        self.assertEqual(bay["capacity"], 6)
        self.assertEqual(bay["name"], "Brewer Street motorcycle bay")

    def test_bad_capacity_is_dropped(self):
        bay = to_bay({"type": "node", "id": 1, "lat": 1, "lon": 1, "tags": {"capacity": "lots"}})
        self.assertNotIn("capacity", bay)

    def test_element_without_position_is_skipped(self):
        self.assertIsNone(to_bay({"type": "node", "id": 404, "tags": {}}))

    def test_build_counts_and_sorts(self):
        data = build({"elements": [
            {"type": "node", "id": 2, "lat": 1, "lon": 1},
            {"type": "node", "id": 1, "lat": 1, "lon": 1},
            {"type": "relation", "id": 9},
        ]})
        self.assertEqual(data["count"], 2)
        self.assertEqual([b["id"] for b in data["bays"]], ["node/1", "node/2"])


if __name__ == "__main__":
    unittest.main()
