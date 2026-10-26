import unittest

from enrich_bays import apply, cache_key, enrich, parse_place, tidy_place


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


class TidyPlaceTest(unittest.TestCase):
    def test_borough_as_area_becomes_area_in_london(self):
        self.assertEqual(tidy_place({"area": "London Borough of Lambeth"}), {"area": "Lambeth", "city": "London"})

    def test_borough_as_city_is_replaced_by_london(self):
        place = tidy_place({"area": "Borough Market", "city": "London Borough of Southwark", "postcode": "SE1"})
        self.assertEqual(place, {"area": "Borough Market", "city": "London", "postcode": "SE1"})

    def test_city_of_westminster_area(self):
        self.assertEqual(tidy_place({"area": "City of Westminster", "postcode": "SW1"}), {"area": "Westminster", "city": "London", "postcode": "SW1"})

    def test_london_postcode_implies_london(self):
        self.assertEqual(tidy_place({"area": "Spitalfields", "postcode": "E1"}), {"area": "Spitalfields", "postcode": "E1", "city": "London"})
        self.assertEqual(tidy_place({"area": "Eastbourne", "postcode": "BN21"}), {"area": "Eastbourne", "postcode": "BN21"})

    def test_town_equal_to_area_is_dropped(self):
        self.assertEqual(tidy_place({"area": "Brighton", "city": "Brighton"}), {"area": "Brighton"})

    def test_other_towns_untouched(self):
        self.assertEqual(tidy_place({"area": "Didsbury", "city": "Manchester"}), {"area": "Didsbury", "city": "Manchester"})


class ApplyTest(unittest.TestCase):
    def test_does_not_overwrite_osm_street(self):
        bay = {"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "Motorcycle parking", "street": "High Street"}
        apply(bay, {"street": "Low Street", "area": "Town", "postcode": "AB1"})
        self.assertEqual(bay["street"], "High Street")
        self.assertEqual(bay["area"], "Town")
        self.assertEqual(bay["postcode"], "AB1")

    def test_refreshes_geocoded_fields(self):
        bay = {"id": "node/1", "lat": 51.5, "lon": -0.1, "name": "Motorcycle parking", "area": "London Borough of Lambeth", "city": "Stale"}
        apply(bay, {"area": "London Borough of Lambeth"})
        self.assertEqual(bay["area"], "Lambeth")
        self.assertEqual(bay["city"], "London")


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
