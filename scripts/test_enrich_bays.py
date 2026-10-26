import unittest

from enrich_bays import apply, cache_key, enrich, parse_place


def photon(**props):
    return {"type": "FeatureCollection", "features": [{"type": "Feature", "properties": props}]}


class ParsePlaceTest(unittest.TestCase):
    def test_building_near_bay(self):
        place = parse_place(photon(
            osm_key="building", street="Old Castle Street", locality="Spitalfields", district="Whitechapel",
            city="London", postcode="E1 7AJ",
        ))
        self.assertEqual(place, {"street": "Old Castle Street", "area": "Spitalfields", "postcode": "E1", "city": "London"})

    def test_road_itself_gives_its_name_as_street(self):
        place = parse_place(photon(osm_key="highway", name="Deansgate", city="Manchester", postcode="M3 2BW"))
        self.assertEqual(place, {"street": "Deansgate", "area": "Manchester", "postcode": "M3"})

    def test_nothing_nearby(self):
        self.assertEqual(parse_place({"features": []}), {})


class ApplyTest(unittest.TestCase):
    def test_does_not_overwrite_osm_street(self):
        bay = {"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "Motorcycle parking", "street": "High Street"}
        apply(bay, {"street": "Low Street", "area": "Town", "postcode": "AB1"})
        self.assertEqual(bay["street"], "High Street")
        self.assertEqual(bay["area"], "Town")
        self.assertEqual(bay["postcode"], "AB1")


class EnrichTest(unittest.TestCase):
    def test_uses_cache_and_only_fetches_new_bays(self):
        bays = [
            {"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "Motorcycle parking"},
            {"id": "node/2", "lat": 51.6, "lon": -0.2, "name": "Motorcycle parking"},
        ]
        cache = {cache_key(bays[0]): {"street": "Cached Street", "area": "Here", "postcode": "C1"}}
        fetched = []

        def fake_fetch(lat, lon):
            fetched.append((lat, lon))
            return {"street": "New Street", "area": "There", "postcode": "N2"}

        enrich(bays, cache, fetch=fake_fetch, pause=0, log=lambda *_: None)
        self.assertEqual(fetched, [(51.6, -0.2)])
        self.assertEqual(bays[0]["street"], "Cached Street")
        self.assertEqual(bays[1]["street"], "New Street")
        self.assertEqual(len(cache), 2)

    def test_failed_lookup_is_retried_next_run(self):
        bays = [{"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "Motorcycle parking"}]
        cache = {}
        enrich(bays, cache, fetch=lambda lat, lon: None, pause=0, log=lambda *_: None)
        self.assertEqual(cache, {})
        self.assertNotIn("street", bays[0])


if __name__ == "__main__":
    unittest.main()
