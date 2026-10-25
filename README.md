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

Both apps store photos and comments **on the device** for now
(`CommunityRepository.kt`, `CommunityStore` in `Stores.swift`). These classes
mirror the backend calls the apps need: submit photo, submit comment, list, and
approve/reject. Replace their bodies with real API calls once a backend is
chosen. "Moderator mode" in Settings is a demo stand-in for real admin accounts.

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

- Replace the placeholder icons. The app id is `uk.motobays.app` on both platforms.
- Connect Firebase for accounts (Google and Apple sign-in), photo storage and moderation.
- Self-host or replace the public Photon search service if traffic grows.
