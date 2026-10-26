#!/usr/bin/env python3
"""Writes one ready-to-send FOI email per council into docs/foi/out/.

Usage:
    python3 docs/foi/generate_emails.py --name "Dwayne Edwards" --address "you@example.com"

Each file starts with To: and Subject: lines, then the body. Councils whose
data is already open or already supplied get a shorter, targeted request.
"""
import argparse
import csv
import pathlib
import re

HERE = pathlib.Path(__file__).parent
TEMPLATE = (HERE / "template.md").read_text(encoding="utf-8")

# The body is everything between the two horizontal rules in template.md.
BODY = TEMPLATE.split("\n---\n")[1].strip()
SUBJECT = re.search(r"\*\*Subject:\*\* (.+)", BODY).group(1)
BODY = BODY.split("\n", 1)[1].strip()

FOLLOW_UP_COORDS = """Dear Information Officer,

In 2026 the council kindly supplied its list of designated motorcycle parking
bays, but without coordinates. Under the Freedom of Information Act 2000 and
the Environmental Information Regulations 2004, please now supply the
coordinates (WGS84 latitude/longitude or British National Grid) for each bay
on that list, as CSV or GeoJSON.

Point locations of council-designated bays are the council's own records, not
Ordnance Survey mapping, and the Public Sector Geospatial Agreement lets
councils release derived point data under the Open Government Licence v3.0.
Please release them under that licence and confirm this in your reply. If the
council still considers the coordinates exempt, please say which exemption
applies and carry out the public interest test.

The data will be published in MotoBays, a free app helping riders find legal
motorcycle parking, and contributed to OpenStreetMap.

Yours faithfully,

{name}
{address}
MotoBays, https://github.com/Dwayne011/Motobay"""

UPDATE_REQUEST = """Dear Information Officer,

The council already publishes its designated motorcycle parking bays as open
data, for which many thanks. Under the Freedom of Information Act 2000, please
send me the current fee status for each bay (free, paid or permit only), the
tariff and payment method where paid, and any time restrictions, as CSV or
XLSX, since these changed after the published extract. Please confirm the data
is released under the Open Government Licence v3.0.

Yours faithfully,

{name}
{address}
MotoBays, https://github.com/Dwayne011/Motobay"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", required=True, help="Your full name")
    ap.add_argument("--address", required=True, help="Postal or email address for correspondence")
    args = ap.parse_args()

    out = HERE / "out"
    out.mkdir(exist_ok=True)
    rows = list(csv.DictReader((HERE / "councils.csv").open(encoding="utf-8")))
    combined = []
    for row in rows:
        council = row["council"]
        status = row["data_status"]
        if status.startswith("partly supplied"):
            body = FOLLOW_UP_COORDS
            subject = "FOI request: coordinates for the motorcycle bay list supplied in 2026"
        elif council.startswith("London Borough of Camden"):
            body = UPDATE_REQUEST
            subject = "FOI request: current fees for designated motorcycle parking bays"
        elif status.startswith("already supplied"):
            continue  # RBKC: nothing to ask for yet
        else:
            body = BODY
            subject = SUBJECT
        text = (
            body.replace("{Council name}", council)
            .replace("{Your full name}", args.name)
            .replace("{name}", args.name)
            .replace("{Your postal or email address for correspondence}", args.address)
            .replace("{address}", args.address)
        )
        email = f"To: {row['foi_email'].strip()}\nSubject: {subject}\n\n{text}\n"
        slug = re.sub(r"[^a-z0-9]+", "-", council.lower()).strip("-")
        (out / f"{slug}.txt").write_text(email, encoding="utf-8")
        combined.append(f"==== {council} ({row['email_status']}) ====\n{email}")
    (out / "ALL-EMAILS.txt").write_text("\n".join(combined), encoding="utf-8")
    print(f"Wrote {len(combined)} emails to {out}")


if __name__ == "__main__":
    main()
