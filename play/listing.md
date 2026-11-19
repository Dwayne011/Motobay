# Google Play listing for MotoBays

Copy each part into Play Console. Character limits are Play's.

## Store settings

- **App name** (30 max): `MotoBays: Motorcycle Parking` (28)
- **Category:** Maps & Navigation
- **Tags:** Navigation, Maps, Motorcycles (pick the closest Play offers)
- **Contact email:** the support address in `docs/privacy.md`
- **Website:** https://github.com/Dwayne011/Motobay
- **Privacy policy:** https://dwayne011.github.io/Motobay/privacy.html
- **Account deletion URL:** https://dwayne011.github.io/Motobay/delete-account.html

## Short description (80 max)

```
Find motorcycle parking bays across the UK, with council charges where known.
```

## Full description (4000 max)

```
MotoBays shows motorcycle parking bays across the UK on one map, so you can find somewhere to park before you set off.

FIND A BAY
• Thousands of motorcycle bays from OpenStreetMap, across the whole UK
• Council details for Camden, Islington, Hillingdon, Bromley, Enfield and Barnet, and free bays across the City of London: prices, charging hours, number of spaces and where exactly the bay is
• Filter by Free, Paid or Unknown, or show only covered bays
• Search any town, street or postcode, and see the nearest bays with walking time
• Fuel prices: the cheapest unleaded and super unleaded near you
• Save a map area for when there's no signal
• Motorcycle garages on the map
• Bays cluster together when you zoom out, so the map stays readable

ON EACH BAY
• Price, spaces, length and whether it's covered
• When it's charged and how to pay, where the council publishes it
• Pay with RingGo in one tap: the location code is copied for you
• "Space now" or "Full" reports from riders, with how long ago
• Ground anchors, CCTV and lighting, as riders report them
• Vehicle crime nearby, from police data
• Riders can report a bay suspended, as well as full or with space
• "Council-checked" on bays from councils' own records
• One tap for directions in your maps app
• Star ratings, photos and comments from other riders

PARKED? WE'LL REMIND YOU
• Tap "I parked here" and MotoBays tells you if the bay is charged right now
• If charging starts later, you get a reminder 15 minutes before
• Where you parked stays on your phone

SAVE YOUR REGULARS
• Star the bays you use most and get to them in a tap
• Saved bays are grouped by price
• A home-screen widget shows the nearest free bay

HELP OTHER RIDERS
• Add a missing bay, or correct one that's wrong
• Add photos and tips: is it easy to find, is it usually full?
• Everything riders post is checked before it appears

PRIVATE BY DESIGN
• No adverts, no analytics, no tracking
• Use it without an account; sign in only to post
• Your location is used on your phone to find nearby bays and is never uploaded
• Delete your account and everything you've posted from inside the app

SUPPORTER (OPTIONAL, ONE-OFF)
MotoBays is free. A one-off Supporter purchase keeps it running and adds:
• "Free now": only the bays you can park in for free at this moment, including paid bays outside their charging hours
• Live charge status on each paid bay: whether it's charged right now, and when that changes

Parking information is guidance only. Councils change and suspend bays without notice, so always check the signs and road markings before you leave your bike.

Map data © OpenStreetMap contributors. Council data under the Open Government Licence.
```

## Graphics

All in `play/`:

| Asset | File | Play requirement |
|---|---|---|
| App icon | `icon-512.png` | 512 x 512 PNG |
| Feature graphic | `feature-graphic.png` | 1024 x 500 PNG or JPEG |
| Phone screenshots | `screenshots/1-explore-map.png` to `5-explore-list.png` | 2 to 8, 1080 x 1920 (9:16) |

Suggested order: 1 map, 2 dark preview card, 3 bay page, 4 saved, 5 list.

The screenshots are the app's real screens with the real bay data over the real map (Bloomsbury, London). No ratings or comments are invented. To regenerate them: `node scripts/store/render_map.mjs android/app/src/test/resources/store` (after converting to JPEG), then `./gradlew testDebugUnitTest --tests "*StoreScreenshotTest*"`; the feature graphic and icon come from `node scripts/store/render_graphics.mjs`.

## Data safety form

Answers that match `docs/privacy.md`. In Play's terms, data that never leaves the phone is not "collected", and data shown to other riders because the rider posted it is not "shared".

**Data collection and security**

- Does your app collect or share any of the required user data types? **Yes**
- Is all of the user data collected by your app encrypted in transit? **Yes**
- Do you provide a way for users to request that their data is deleted? **Yes** (in the app, and at the account deletion URL)

**Data types collected** (none are shared; all are optional except where noted)

| Play data type | Collected | Shared | Required? | Purposes |
|---|---|---|---|---|
| Personal info › Name (display name) | Yes | No | Optional | App functionality, Account management |
| Personal info › Email address | Yes | No | Optional (only when signing in) | Account management |
| Personal info › User IDs | Yes | No | Optional (only when signing in) | App functionality, Account management, Fraud prevention, security and compliance |
| Photos and videos › Photos | Yes | No | Optional | App functionality |
| App activity › Other user-generated content (comments, star ratings, space and safety reports, bays and corrections, reports) | Yes | No | Optional | App functionality, Fraud prevention, security and compliance |
| App info and performance › Crash logs | Yes | No | Optional (only if the rider emails a report) | Analytics: no. Choose App functionality |
| App info and performance › Diagnostics (device model, Android version) | Yes | No | Optional (same email report) | App functionality |

**Not collected:**
- Location: used on the phone only, never sent.
- Financial info and purchase history: Google Play handles the purchase, and the app keeps only a yes/no answer on the phone.
- Contacts, messages, files, calendar, health and audio.
- Web browsing.
- Advertising and other device IDs.

Firebase App Check sends a Play Integrity token to protect the backend. Google's Firebase guidance treats this as data handled by Google Play services, not by the app. Check Firebase's current Data safety page when you fill the form in, in case it has changed.

## Content rating questionnaire

- **Category:** All other app types (not a game)
- **Violence, sexual content, language, drugs, gambling:** No
- **Does the app allow users to interact or exchange content?** Yes. Riders post photos and comments.
  - Moderated before publishing: Yes
  - Users can report content: Yes
  - Users can block other users: Yes
- **Does the app share the user's physical location with other users?** No
- **Digital purchases:** Yes (the optional Supporter in-app product)

## Target audience and content

- **Target age:** 16-17 and 18 and over. The privacy policy says the app is for riders aged 16 and over.
- **Appeals to children:** No
- **Ads:** No, the app contains no ads
- **News app:** No
- **Government app:** No
- **Financial features:** None
- **Health:** No

## In-app product

- **Product ID:** `supporter` (must match exactly)
- **Type:** One-time, non-consumable
- **Name:** MotoBays Supporter
- **Description:** Unlocks the "Free now" filter and live charge status, and keeps MotoBays free with no adverts.
- **Price:** £2.99
