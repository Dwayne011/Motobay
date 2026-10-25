# MotoBay (working name)

Find motorcycle parking across the UK, get directions, and share photos and
comments. Everything riders submit is held for moderator review first.

React + TypeScript + Vite, using **MUI v6** themed to **Material Design 3**.
One codebase runs on the web and is wrapped for **iOS and Android** with Capacitor.

## Run it

```bash
npm install
npm run dev                            # live OpenStreetMap data
VITE_DATA_SOURCE=sample npm run dev    # offline, made-up sample bays
npm run build
```

## Where the data comes from

- **Bays:** OpenStreetMap, queried live through the Overpass API
  (`amenity=motorcycle_parking`, plus car parks tagged `motorcycle=designated`).
  Loaded in tiles once zoomed in to level 13 or closer. Covers the whole UK.
- **Place search:** Nominatim, limited to `gb`.
- Attribution "© OpenStreetMap contributors" is required and shown in the app.
- The public Overpass and Nominatim servers and OSM map tiles are for light use only.
  Before launch, switch to a self-hosted or paid provider (set `VITE_OVERPASS_URL`).

## Community content and moderation

`src/data/community.ts` is a **local mock** that stores data in the browser. It
mirrors the backend calls the UI needs (submit photo/comment, list, moderate).
Replace those functions with real API calls once a backend is chosen. The
"Moderator mode" switch in Settings is a demo stand-in for real role-based access.

## Material Design 3 implementation

| Requirement | Where |
|---|---|
| Dynamic tonal palettes from one source colour (`SchemeTonalSpot`), dark mode uses inverted tones | `src/theme/m3Scheme.ts` |
| MUI v6 CSS theme variables + light/dark `colorSchemes` | `src/theme/createM3Theme.ts` |
| Tonal elevation (surface + primary tint, levels 0–5), no drop shadows | `src/theme/elevation.ts`, `MuiPaper` override |
| M3 type scale only: Display / Headline / Title / Body / Label (MD2 variants are TypeScript errors) | `src/theme/typography.ts`, `src/theme/augment.d.ts` |
| M3 buttons: filled, tonal, elevated, outlined, text (`contained` removed) | `MuiButton` variants |
| Navigation rail (≥600dp) and Navigation bar (<600dp) | `src/components/m3/navigation.tsx`, `src/AppShell.tsx` |
| Segmented buttons | `src/components/m3/SegmentedButton.tsx` |
| Search bar, switch, chips, FAB, dialogs, snackbar, cards | `src/components/m3/SearchBar.tsx`, theme overrides |
| Window size classes (compact / medium / expanded) and list-detail layout | `src/hooks/useWindowSizeClass.ts`, `ExplorePage.tsx` |

MUI v6 has no M3 Navigation rail, Navigation bar or Segmented button, so these
are built to the M3 specs on top of MUI primitives.

The one exception to "no shadows" is elements floating over the map (FAB, pins,
menus, snackbar). The M3 spec gives these a key shadow because a tonal tint
can't separate them from map imagery.

## iOS and Android (Capacitor)

Requires Xcode (iOS) and Android Studio (Android) on your machine.

```bash
npm run build
npx cap add ios
npx cap add android
npx cap sync
npx cap open ios        # or: npx cap open android
```

Then add the location permission text:
- iOS `Info.plist`: `NSLocationWhenInUseUsageDescription`
- Android `AndroidManifest.xml`: `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`
