# FOI request template: motorcycle parking bays

Fill in the parts in `{curly braces}`. Keep it short: councils answer narrow,
clearly scoped requests fastest. The wording follows the request that four
London boroughs (Camden, Lambeth, Tower Hamlets, RBKC) answered in 2026, with
two additions: a request for the data's licence to be confirmed in writing, and
a note that an Environmental Information Regulations (EIR) request is being
made at the same time, which some councils prefer for highways data.

---

**Subject:** Freedom of Information request: designated motorcycle parking bays

Dear Information Officer,

I am writing to make a request under the Freedom of Information Act 2000 and,
to the extent it applies, the Environmental Information Regulations 2004.

Please send me the council's current list of designated motorcycle parking
bays (solo motorcycle bays and bays shared with mopeds, scooters or other
vehicles) within {Council name}. For each bay, where the council holds it,
please include:

1. Location: coordinates (WGS84 latitude/longitude or British National Grid
   easting/northing) and the street or site name
2. Postcode
3. Capacity (number of motorcycles)
4. Bay type (solo motorcycle only, shared with mopeds/scooters, or shared
   with another vehicle type)
5. Restrictions (times of day, days of the week, or permit requirements)
6. Fee status (free, paid or permit only) and, if paid, the tariff and
   payment method
7. Whether the bay is covered or has security anchors, if recorded
8. The date the record was last updated

I am only asking for information the council already holds; I do not need it
to be created or checked. Please supply it as data rather than a document:
CSV (UTF-8) is ideal, otherwise GeoJSON, Excel (XLSX) or a zipped Shapefile
with its .prj file. Please do not send PDFs or scanned images.

Please release the data under the Open Government Licence v3.0, and confirm
in your reply that it may be re-used under that licence. The purpose is to
publish bay locations in MotoBays, a free app that helps riders find legal
motorcycle parking, and to contribute them to OpenStreetMap. Accurate bay
data reduces pavement parking and penalty notices, so I hope the council will
see a shared benefit.

If any part of this request would exceed the cost limit, please supply the
parts that can be provided within it and tell me which part is the problem,
so I can narrow the request.

Please acknowledge receipt. I understand you have 20 working days to respond.

Yours faithfully,

{Your full name}
{Your postal or email address for correspondence}
MotoBays, https://github.com/Dwayne011/Motobay

---

## If they refuse or hold back coordinates

Tower Hamlets gave street names but withheld coordinates, claiming third-party
rights (usually meaning Ordnance Survey data). A short follow-up usually
works:

> Thank you for the list. Could you also supply the coordinates? Point
> locations of council-designated bays are the council's own data; they are
> not Ordnance Survey mapping, and the Public Sector Geospatial Agreement
> (PSGA) in any case lets councils release derived point data under the OGL.
> If the council still considers the coordinates exempt, please confirm which
> exemption applies and carry out the public interest test.

## If there's no reply after 20 working days

Send one line asking for an internal review. If nothing comes within another
20 working days, complain to the Information Commissioner's Office
(ico.org.uk/make-a-complaint). Councils answer promptly once an ICO reference
exists.
