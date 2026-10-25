/** App-wide settings. The name is a placeholder until a final brand is chosen. */
export const APP_NAME = 'MotoBay';

/** Default M3 source colour. Every palette in the app is derived from this. */
export const DEFAULT_SOURCE_COLOR = '#0B6E99';

/** Preset source colours offered in Settings. */
export const SOURCE_COLOR_PRESETS = ['#0B6E99', '#6750A4', '#B3261E', '#386A20', '#8B5000', '#006A6A'];

/**
 * Where bay data comes from:
 * - "overpass": live OpenStreetMap data for the whole UK (default)
 * - "sample": a few made-up bays for offline development
 */
export const DATA_SOURCE: 'overpass' | 'sample' =
  import.meta.env.VITE_DATA_SOURCE === 'sample' ? 'sample' : 'overpass';

/** Public Overpass instance. Production should use a self-hosted or paid instance. */
export const OVERPASS_URL = import.meta.env.VITE_OVERPASS_URL ?? 'https://overpass-api.de/api/interpreter';

export const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';

/** Bays only load at this zoom or closer, to keep Overpass queries small. */
export const MIN_BAY_ZOOM = 13;

/** Initial map view: Great Britain and Northern Ireland. */
export const UK_CENTER: [number, number] = [54.5, -3.0];
export const UK_ZOOM = 6;
