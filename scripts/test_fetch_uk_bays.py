import unittest

from fetch_uk_bays import build, to_bay, with_council, without_council


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

    def test_neighbouring_spaces_merge_into_one_bay(self):
        space = lambda i, lat: {"type": "way", "id": i, "center": {"lat": lat, "lon": -0.1},
                                "tags": {"amenity": "parking_space", "parking_space": "motorcycle"}}
        data = build({"elements": [space(1, 51.50000), space(2, 51.50005), space(3, 51.50010),
                                   space(4, 51.51000)]})
        self.assertEqual(data["count"], 2)
        self.assertEqual(sorted(b["capacity"] for b in data["bays"]), [1, 3])

    def test_space_next_to_existing_bay_is_dropped(self):
        data = build({"elements": [
            {"type": "node", "id": 1, "lat": 51.5, "lon": -0.1, "tags": {"amenity": "motorcycle_parking"}},
            {"type": "way", "id": 2, "center": {"lat": 51.50005, "lon": -0.1},
             "tags": {"amenity": "parking_space", "parking_space": "motorcycle"}},
        ]})
        self.assertEqual([b["id"] for b in data["bays"]], ["node/1"])




class CouncilBaysTest(unittest.TestCase):
    COUNCIL = {
        "sources": {"hillingdon": "Hillingdon Council traffic orders (FOI 06397)"},
        "bays": [
            {"id": "council/hillingdon/a", "source": "hillingdon", "lat": 51.5731, "lon": -0.4235, "street": "High Street",
             "lengthMetres": 7.7, "openingHours": "Any time", "maxStay": "20 minutes", "note": "South-west side.", "approximate": True},
            {"id": "council/hillingdon/b", "source": "hillingdon", "lat": 51.6, "lon": -0.41, "street": "Joel Street", "note": "West side."},
        ],
        "unplaced": [{"description": "somewhere"}],
    }

    def test_council_bays_are_added_as_free_but_unconfirmed(self):
        data = with_council([], self.COUNCIL)
        self.assertEqual(data["count"], 2)
        a = data["bays"][0]
        self.assertEqual(a["id"], "council/hillingdon/a")
        self.assertEqual(a["fee"], "free")
        self.assertTrue(a["feeUnconfirmed"])
        self.assertNotIn("capacity", a)
        self.assertEqual(a["source"], "Hillingdon Council traffic orders (FOI 06397)")
        self.assertEqual(a["note"], "South-west side.")
        self.assertTrue(a["approximate"])
        self.assertIn("Hillingdon", data["attribution"])

    def test_a_source_that_confirmed_it_is_free_drops_the_unconfirmed_flag(self):
        council = dict(self.COUNCIL, sources={"hillingdon": {"name": "Hillingdon Council traffic orders (FOI 06397)", "feeConfirmed": True}})
        data = with_council([], council)
        self.assertTrue(all(b["fee"] == "free" and "feeUnconfirmed" not in b for b in data["bays"]))
        self.assertEqual(data["bays"][0]["source"], "Hillingdon Council traffic orders (FOI 06397)")
        self.assertIn("Hillingdon Council traffic orders (FOI 06397)", data["attribution"])

    def test_capacity_is_kept_when_the_council_gives_one(self):
        council = {"sources": {"bromley": "Bromley"}, "bays": [
            {"id": "council/bromley/x", "source": "bromley", "lat": 51.4, "lon": 0.01, "capacity": 12, "note": "Mouth of the road."}]}
        self.assertEqual(with_council([], council)["bays"][0]["capacity"], 12)

    def test_a_bay_openstreetmap_already_has_keeps_its_id_and_takes_the_council_details(self):
        osm = [{"id": "node/1", "lat": 51.57312, "lon": -0.42352, "name": "Motorcycle parking", "fee": "unknown"}]
        data = with_council(osm, self.COUNCIL)
        self.assertEqual([b["id"] for b in data["bays"]], ["council/hillingdon/b", "node/1"])
        node = data["bays"][1]
        self.assertEqual((node["lat"], node["lon"]), (51.57312, -0.42352))
        self.assertEqual(node["note"], "South-west side.")
        self.assertEqual(node["maxStay"], "20 minutes")
        self.assertEqual(node["fee"], "free")
        self.assertTrue(node["feeUnconfirmed"])
        self.assertIn("Hillingdon", data["attribution"])

    def test_an_unconfirmed_council_price_does_not_override_openstreetmap(self):
        osm = [{"id": "node/1", "lat": 51.57312, "lon": -0.42352, "name": "x", "fee": "paid"}]
        node = [b for b in with_council(osm, self.COUNCIL)["bays"] if b["id"] == "node/1"][0]
        self.assertEqual(node["fee"], "paid")
        self.assertNotIn("feeUnconfirmed", node)

    def test_paid_council_bays_carry_their_rules(self):
        council = {"sources": {"islington": {"name": "Islington", "feeConfirmed": True}}, "bays": [
            {"id": "council/islington/a-1", "source": "islington", "lat": 51.55, "lon": -0.1, "fee": "paid", "capacity": 6, "rules": "Paid Mon–Fri."},
            {"id": "council/islington/a-2", "source": "islington", "lat": 51.55001, "lon": -0.1, "fee": "paid", "capacity": 9, "rules": "Paid Sat."}]}
        osm = [{"id": "node/7", "lat": 51.55, "lon": -0.10001, "name": "x", "fee": "free", "capacity": 2}]
        bays = {b["id"]: b for b in with_council(osm, council)["bays"]}
        # The confirmed council price wins; one OpenStreetMap bay is matched once, the second council bay is added.
        self.assertEqual(bays["node/7"]["fee"], "paid")
        self.assertEqual(bays["node/7"]["capacity"], 6)
        self.assertEqual(bays["node/7"]["rules"], "Paid Mon–Fri.")
        self.assertEqual(bays["council/islington/a-2"]["fee"], "paid")
        self.assertNotIn("feeUnconfirmed", bays["council/islington/a-2"])

    def test_council_details_can_be_taken_off_again(self):
        osm = [{"id": "node/1", "lat": 51.57312, "lon": -0.42352, "name": "Motorcycle parking", "fee": "unknown", "capacity": 3}]
        original = [dict(b) for b in osm]
        merged = with_council(osm, self.COUNCIL)["bays"]
        self.assertEqual(without_council(merged), original)
        # Merging again from the restored bays gives the same file.
        self.assertEqual(with_council(without_council(merged), self.COUNCIL)["bays"], merged)

    def test_no_council_file_leaves_osm_alone(self):
        osm = [{"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "x", "fee": "free"}]
        data = with_council(osm, None)
        self.assertEqual(data["bays"], osm)
        self.assertEqual(data["attribution"], "© OpenStreetMap contributors, ODbL 1.0")


if __name__ == "__main__":
    unittest.main()
