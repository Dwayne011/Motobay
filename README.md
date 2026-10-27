# MotoBays

Find motorcycle parking across the UK, get directions, and share photos and
comments. Everything riders submit is held for moderator review first.

Two fully native apps:

| | Android | iOS |
|---|---|---|
| Folder | `android/` | `ios/` |
| Language / UI | Kotlin, Jetpack Compose | Swift, SwiftUI |
| Design | Material Design 3 | Apple Human Interface Guidelines |
| Map | MapLibre with OpenFreeMap tiles (no API key) | Apple MapKit |
| Minimum OS | Android 8.0 (API 26), targets API 36 | iOS 17 |

## Where the data comes from

- **Bays:** OpenStreetMap. A GitHub Action (`.github/workflows/update-bay-data.yml`)
  runs `scripts/fetch_uk_bays.py` every Monday and publishes every UK
  motorcycle bay to `data/uk-bays.json` (about 50 KB compressed). The apps
  download that one file, keep it on the device and only re-download it when
  it changes, so phones never query the shared Overpass servers.
  Run the job by hand from the repo's Actions tab ("Update bay data" > Run workflow).
- **Coverage:** only as good as OpenStreetMap. London is well mapped (about
  1,400 bays); many other cities have few or none yet.
- **Place search (suggestions as you type):** Android uses Photon (OpenStreetMap)
  plus postcodes.io for postcodes; iOS uses Apple's own MapKit search completer.
  Photon and postcodes.io are free public services with fair-use limits.
- "© OpenStreetMap contributors" attribution is required and shown in both apps.

## Community content and moderation

Riders sign in (Google or Apple), then submit photos, comments, new bays and
bay corrections. Everything starts as *Pending*, the admin's own submissions
included; only the admin can approve, reject or delete it, and only approved
items are public. Riders also see their own pending items.

- **Android** uses Firebase Auth, Cloud Firestore (the database named
  `motobays`, in `eur3`) and Cloud Storage (`FirestoreCommunityRepository.kt`, `FirebaseAuthRepository`). The
  Review tab appears only for the admin account (`Moderators.kt`).
- **iOS** still keeps photos and comments on the device (`CommunityStore` in
  `Stores.swift`) and has its `GoogleService-Info.plist` ready for when it is
  switched over.
- **Rules** are in `firestore.rules` and `storage.rules`. The admin's email is
  listed in both and in `Moderators.kt`; a unit test checks they agree.
  Deploy them with `firebase deploy --only firestore:rules,storage` (project
  `motobays-3405f`, set in `.firebaserc`).
- **Rules tests** run against the Firebase emulators:
  `cd rules-tests && npm install && npm test` (needs Java).
- Photos reach other riders through a download URL stored on the Firestore
  photo document, which is only readable once the photo is approved. The files
  of rejected photos are not deleted automatically.
- Apple sign-in on Android needs the Apple provider enabled in the Firebase
  console (Authentication > Sign-in method).

## Android

Open `android/` in Android Studio, or build from the command line:

```bash
cd android
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest        # screenshot tests -> app/build/outputs/roborazzi/
./gradlew assembleDebug -Pmotobay.sampleData=true   # offline, made-up sample bays
```

How Material Design 3 is applied:

| Requirement | Where |
|---|---|
| All colours from one seed colour (M3 tonal spot), dark mode uses inverted tones; optional wallpaper colours on Android 12+ | `ui/theme/Theme.kt` |
| Tonal elevation (surface + primary tint), not shadows | Compose M3 `Surface(tonalElevation)` and surfaceContainer roles |
| M3 type scale (Display, Headline, Title, Body, Label) | `MaterialTheme.typography` |
| Navigation bar on phones, Navigation rail on tablets | `NavigationSuiteScaffold` in `ui/MotoBayApp.kt` |
| Segmented buttons for quick choices (price filter, map/list, theme, review type) | `SingleChoiceSegmentedButtonRow` |
| List-detail layout on large screens | `ui/explore/ExploreScreen.kt` |

## iOS

Requires a Mac with Xcode 16 or later. Open `ios/MotoBay.xcodeproj`, pick your
team under Signing & Capabilities, and run. To use made-up sample bays, edit the
MotoBay scheme and tick the `MOTOBAY_SAMPLE_DATA` environment variable.

Follows iOS conventions: tab bar, large titles, Apple Maps, SF Symbols, system
photo picker and camera, and swipe actions. Bay details open in a sheet on
iPhone and a side panel (inspector) on iPad. Directions open in Apple Maps,
Google Maps or Waze.

## Before publishing

- Set `PRIVACY_POLICY_URL` in `android/app/src/main/java/uk/motobay/app/data/Legal.kt` to the hosted UK GDPR policy, and give
  Play the same URL plus a web page for account-deletion requests (Play Console > App content > Data safety and Account deletion).
- Replace the placeholder iOS icon (Android has its pin-and-motorcycle icon). The app id is `uk.motobays.app` on both platforms.
- Finish the Firebase setup: enable the Apple provider, move iOS onto Firebase, and add the upload key's SHA-1 to the Android app
  (and to the Firebase API key's Android restriction in Google Cloud).
- Self-host or replace the public Photon search service if traffic grows.

## Releasing to Google Play

Signing: create an upload keystore once and keep it out of git (`.gitignore` already covers it):

```bash
cd android
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
printf 'storeFile=upload-keystore.jks\nstorePassword=...\nkeyAlias=upload\nkeyPassword=...\n' > keystore.properties
keytool -list -v -keystore upload-keystore.jks -alias upload | grep SHA1   # register in Firebase + Google Cloud key restriction
```

Build and check:

```bash
./gradlew bundleRelease -Pmotobay.versionCode=17 -Pmotobay.versionName=1.0.1   # app/build/outputs/bundle/release/app-release.aab
./gradlew testDebugUnitTest
# Permissions in the final manifest must be INTERNET, ACCESS_NETWORK_STATE, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION only
# (plus the app-private DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION that androidx adds):
grep uses-permission app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

`versionCode` must go up on every upload; without `-Pmotobay.versionCode` it uses the default in `app/build.gradle.kts`.
Release builds are minified with R8 and only signed when `keystore.properties` exists.
