import json
import os
import tempfile
import unittest
from unittest import mock

import fetch_open_council_bays as m


class HelpersTest(unittest.TestCase):
    def test_sentence_joins_order_pieces(self):
        self.assertEqual(
            m.sentence("north side", "From a point 6 metres east of  Park Road.", "for a distance of 7 metres."),
            "North side, from a point 6 metres east of Park Road, for a distance of 7 metres.",
        )
        self.assertIsNone(m.sentence("", None))

    def test_stated_length_comes_from_the_order(self):
        self.assertEqual(m.stated_length("for a distance of 6.5 metres in a westerly direction"), 6.5)
        self.assertIsNone(m.stated_length("to its junction with Lady Lane"))

    def test_street_names(self):
        self.assertEqual(m.tidy_street("THORESBY PLACE, CITY CENTRE"), "Thoresby Place")
        self.assertEqual(m.tidy_street("COOKRIDGE STREET (NORTHERN SECTION), CITY CENTRE"), "Cookridge Street")


class MainTest(unittest.TestCase):
    def run_main(self, fetchers, previous=None):
        with tempfile.TemporaryDirectory() as d:
            out = os.path.join(d, "open-council-bays.json")
            if previous is not None:
                with open(out, "w") as f:
                    json.dump(previous, f)
            with mock.patch.object(m, "FETCHERS", fetchers), mock.patch("sys.argv", ["x", out]):
                m.main()
            with open(out) as f:
                return json.load(f)

    def bay(self, source, n):
        return {"id": f"council/{source}/{n}", "source": source, "lat": 55.1234567, "lon": -3.1, "fee": "unknown"}

    def test_a_council_that_fails_keeps_last_weeks_bays(self):
        def broken():
            raise OSError("down")
        previous = {"bays": [self.bay("leeds", 1)]}
        data = self.run_main({"edinburgh": lambda: [self.bay("edinburgh", 2)], "leeds": broken}, previous)
        self.assertEqual([b["id"] for b in data["bays"]], ["council/edinburgh/2", "council/leeds/1"])
        self.assertEqual(data["bays"][0]["lat"], 55.123457)

    def test_every_source_is_credited(self):
        data = self.run_main({"edinburgh": lambda: [self.bay("edinburgh", 2)]})
        self.assertIn("Open Government Licence", data["sources"]["edinburgh"]["name"])
        self.assertIn("contains Ordnance Survey data", data["sources"]["edinburgh"]["name"])


if __name__ == "__main__":
    unittest.main()
