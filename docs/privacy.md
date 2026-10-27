---
title: MotoBays privacy policy
---

# MotoBays privacy policy

**App:** MotoBays (`uk.motobays.app`, Android) · **Last updated:** 27 October 2026

MotoBays helps riders find motorcycle parking bays in the UK. This page explains what the app collects, why, where it goes, and how to get it deleted. It is written to meet the UK GDPR and the Data Protection Act 2018, and it matches what we declare in the Google Play Data safety form.

## Who is responsible

The data controller is the MotoBays developer. Contact: **dwayne.edwards89@googlemail.com**. If you are unhappy with how we handle your data you can also complain to the Information Commissioner's Office (ico.org.uk).

## What the app collects, and why

| Data | When | Why | Where it is kept |
|---|---|---|---|
| **Location** (precise or approximate, your choice) | Only when you tap the "my location" button, or search while location is allowed | To centre the map near you and show distances | On your phone only. It is never uploaded or logged. |
| **Account** – Google or Apple sign-in: a unique user ID, your email address, and whether it is verified | When you choose to sign in | So you can post photos, comments and bay changes, see their review status, and so moderators can act on abuse | Firebase Authentication (Google; a global service, not region-pinned) |
| **Display name** you choose | When you save one | Shown next to your posts; kept unique across riders | Cloud Firestore (Google), EU multi-region `eur3` |
| **Photos, comments, new bays and corrections** you submit, with the time and your user ID | When you post them | Shown to other riders once a moderator approves them | Cloud Firestore (EU, `eur3`) and Cloud Storage for Firebase (Google), in the region chosen when the project was set up |
| **Reports** you make about other riders' content | When you tap Report | So a moderator can review it | Cloud Firestore; only the moderator can see them |
| **Saved bays, settings, blocked riders, recent searches** | As you use the app | Your preferences | On your phone only (included in Android backup if you have it on) |
| **Crash report** (device model, Android version, app version, a stack trace) | Only if the app crashes and you choose to send it | To fix the bug | Sent by email to the address above, only when you tap Send |

The app contains no advertising, no analytics SDK and no tracking identifiers. It does not read your contacts, photos library, camera roll or files: photos are picked one at a time through Android's photo picker or camera, and the app only receives the picture you choose. Photos are re-encoded before upload, which strips location and camera metadata.

## Services the app talks to

| Service | What it receives | Their policy |
|---|---|---|
| **Firebase** (Google LLC) – Authentication, Firestore, Storage, App Check | Everything in the "Account" and "posts" rows above, plus a device attestation token so only the genuine app can use the backend | https://firebase.google.com/support/privacy |
| **OpenFreeMap** (map tiles) | The map area you look at (tile coordinates) and your IP address | https://openfreemap.org |
| **Photon by komoot** (place search) | The text you type into search and your IP address | https://photon.komoot.io |
| **postcodes.io** (postcode search) | Postcodes you type and your IP address | https://postcodes.io |
| **GitHub** (bay data file) | A request for the bay data file and your IP address | https://docs.github.com/site-policy |

Each request carries the app's name and version as its User-Agent so the service can identify it. None of these services receive your account details or location fix.

## Legal basis (UK GDPR Article 6)

- **Consent** for location (the Android permission prompt) and for sending a crash report.
- **Performance of a contract** – running the account you asked for and showing your posts.
- **Legitimate interests** – keeping the service safe: moderation, reports, blocking and App Check.

## Who can see what

- Approved photos, comments and bay changes are public to everyone using the app, shown with your display name (or "Anonymous rider").
- Pending and rejected posts are visible only to you and the moderator.
- Your email address is visible only to the moderator, never to other riders.

## How long we keep it

- Posts stay until you delete them, the moderator removes them, or you delete your account.
- Location is never stored.
- Firebase sign-in records are deleted when you delete your account.

## Your rights

You can access, correct, export or delete your data, object to processing, or withdraw consent at any time. Most of this you can do yourself in the app: Settings › My submissions lists everything you have posted; Settings › Account › Delete account & data removes all of it. For anything else email the address above; we reply within one month.

## Deleting your account

See [Delete your MotoBays account](delete-account.html). In short: **Settings › Account › Delete account & data** removes your sign-in, display name and every post, immediately and permanently.

## Children

MotoBays is for riders aged 16 or over. We do not knowingly collect data from anyone younger.

## Changes

We will update this page and the "last updated" date if anything changes. Significant changes are also announced in the app.
