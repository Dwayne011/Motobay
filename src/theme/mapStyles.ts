import { m3, m3Shape } from './createM3Theme';
import { floatingShadow } from './elevation';
import { m3TypeScale } from './typography';

/** Leaflet styling in M3 colour roles. Map pins are plain HTML, so they're styled globally. */
export const m3Pins = {
  '.leaflet-container': {
    fontFamily: m3TypeScale.bodyMedium.fontFamily,
    backgroundColor: m3('surfaceContainer'),
  },
  // OSM tiles have no dark style, so invert them in dark mode.
  '.dark .leaflet-tile-pane': {
    filter: 'invert(1) hue-rotate(180deg) brightness(0.9) contrast(0.9)',
  },
  '.leaflet-control-attribution': {
    ...m3TypeScale.labelSmall,
    backgroundColor: `color-mix(in srgb, ${m3('surface')} 85%, transparent) !important`,
    color: m3('onSurfaceVariant'),
    borderTopLeftRadius: m3Shape.small,
    '& a': { color: m3('primary') },
  },
  '.bay-pin': {
    ...m3TypeScale.labelLarge,
    width: '100%',
    height: '100%',
    boxSizing: 'border-box',
    borderRadius: m3Shape.full,
    display: 'grid',
    placeItems: 'center',
    border: `2px solid ${m3('surface')}`,
    boxShadow: floatingShadow,
    transition: 'transform 150ms',
  },
  '.bay-pin--free': { backgroundColor: m3('success'), color: m3('onSuccess') },
  '.bay-pin--paid': { backgroundColor: m3('secondary'), color: m3('onSecondary') },
  '.bay-pin--unknown': { backgroundColor: m3('outline'), color: m3('surface') },
  '.bay-pin--selected': {
    backgroundColor: m3('primary'),
    color: m3('onPrimary'),
    border: `3px solid ${m3('primaryContainer')}`,
    ...m3TypeScale.titleMedium,
  },
  '.leaflet-marker-icon:focus-visible .bay-pin': {
    outline: `3px solid ${m3('primary')}`,
    outlineOffset: 2,
  },
  '.user-location': {
    fill: m3('primary'),
    stroke: m3('surface'),
  },
};
