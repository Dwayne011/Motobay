# Getting bay data from councils (FOI)

OpenStreetMap only has what volunteers have mapped. Councils hold the complete
list of bays they've designated, and the Freedom of Information Act obliges
them to hand it over. This folder is the kit for asking all of them.

## What's here

| File | Purpose |
|---|---|
| `template.md` | The request wording, plus follow-ups for refusals and silence |
| `councils.csv` | The 33 London councils: FOI inbox, whether it's verified, what data we already have, and columns to track each request |
| `generate_emails.py` | Produces one personalised email per council in `out/`, plus `out/ALL-EMAILS.txt` |

## How to send

```bash
python3 docs/foi/generate_emails.py --name "Your Name" --address "you@example.com"
```

Then, for each file in `docs/foi/out/`, paste the To, Subject and body into
your email client. Before sending, open the council's own FOI page and check
the address: only Westminster, Islington and City of London have been
verified; the rest come from a third-party directory and inboxes change.

**Recommended instead of email: WhatDoTheyKnow.** Make each request at
whatdotheyknow.com (search the council, click "Make a request", paste the
body). It sends to the council's current address, nags them when the deadline
passes, and publishes the reply, which is useful later as proof the data was
released openly. The requests also become a public record other riders can
find.

Keep requests in your own name. Councils don't need to know about the app,
though the template mentions it because a stated public purpose tends to get
friendlier answers.

## What to expect

- **Acknowledgement** within a few days; **answer** within 20 working days.
  Some councils will send the data in a week; some will wait until day 20.
- **Good replies** are a CSV or spreadsheet with coordinates. Put them in
  `data/council/<borough>/` and the data pipeline will merge them in (to be
  built once the first replies arrive).
- **Street names without coordinates:** send the coordinates follow-up from
  `template.md`. In the meantime the pipeline can geocode street names, as
  ParkMoto did for Lambeth and Tower Hamlets, but accuracy suffers.
- **PDF or map image:** reply asking for the underlying data, which they must
  provide if held electronically.
- **Refusal under section 12 (cost):** narrow the request to coordinates,
  street name and fee only.
- **Refusal under section 21 (already published):** ask them to point to the
  published dataset and confirm its licence. If it's under an Ordnance Survey
  licence rather than OGL (as City of London's is), say the OS licence doesn't
  permit re-use and ask for OGL release.

## Licensing

Ask for Open Government Licence v3.0 every time and keep the written
confirmation. OGL data can be published in the app and contributed to
OpenStreetMap. Data with no stated licence, or under an OS licence, can't be.

## Beyond London

The same template works for any UK council. Bays are held by the highway
authority: county councils and unitary authorities in England, and every
council in Scotland, Wales and Northern Ireland. That's roughly 150 bodies
rather than 300+, because English district councils don't manage on-street
parking. Add rows to `councils.csv` and re-run the script.

## Tracking

Fill in `sent_on`, `reply_due` (sent date + 20 working days) and `outcome` in
`councils.csv` as you go. Commit it, so the history of who sent what stays
with the project.
