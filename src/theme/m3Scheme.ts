import {
  argbFromHex,
  Blend,
  Hct,
  hexFromArgb,
  MaterialDynamicColors,
  SchemeTonalSpot,
  TonalPalette,
} from '@material/material-color-utilities';

/**
 * Every M3 colour role the app uses. All of them are derived from one source
 * colour using the M3 "tonal spot" scheme, so primary, secondary and tertiary
 * are tonal palettes of the same seed.
 * https://m3.material.io/styles/color/roles
 */
export const M3_ROLES = [
  'primary',
  'onPrimary',
  'primaryContainer',
  'onPrimaryContainer',
  'secondary',
  'onSecondary',
  'secondaryContainer',
  'onSecondaryContainer',
  'tertiary',
  'onTertiary',
  'tertiaryContainer',
  'onTertiaryContainer',
  'error',
  'onError',
  'errorContainer',
  'onErrorContainer',
  'surface',
  'onSurface',
  'surfaceVariant',
  'onSurfaceVariant',
  'surfaceDim',
  'surfaceBright',
  'surfaceContainerLowest',
  'surfaceContainerLow',
  'surfaceContainer',
  'surfaceContainerHigh',
  'surfaceContainerHighest',
  'inverseSurface',
  'inverseOnSurface',
  'inversePrimary',
  'outline',
  'outlineVariant',
  'surfaceTint',
  'shadow',
  'scrim',
] as const;

export type M3Role = (typeof M3_ROLES)[number];
export type M3Scheme = Record<M3Role, string>;

/** Extended "custom colours", harmonised towards the source colour as M3 recommends. */
export interface M3CustomColor {
  color: string;
  onColor: string;
  container: string;
  onContainer: string;
}

export interface M3SchemePair {
  light: M3Scheme;
  dark: M3Scheme;
  success: { light: M3CustomColor; dark: M3CustomColor };
  warning: { light: M3CustomColor; dark: M3CustomColor };
}

function buildScheme(sourceArgb: number, isDark: boolean): M3Scheme {
  const scheme = new SchemeTonalSpot(Hct.fromInt(sourceArgb), isDark, 0);
  const out = {} as M3Scheme;
  for (const role of M3_ROLES) {
    out[role] = hexFromArgb(MaterialDynamicColors[role].getArgb(scheme));
  }
  return out;
}

function customColor(hex: string, sourceArgb: number): M3SchemePair['success'] {
  const harmonised = Blend.harmonize(argbFromHex(hex), sourceArgb);
  const palette = TonalPalette.fromInt(harmonised);
  const tone = (t: number) => hexFromArgb(palette.tone(t));
  return {
    light: { color: tone(40), onColor: tone(100), container: tone(90), onContainer: tone(10) },
    dark: { color: tone(80), onColor: tone(20), container: tone(30), onContainer: tone(90) },
  };
}

/**
 * Builds light and dark schemes from the same source colour. Dark mode uses
 * the inverted tones of the same palettes (for example primary is tone 40 in
 * light and tone 80 in dark).
 */
export function generateSchemes(sourceHex: string): M3SchemePair {
  const sourceArgb = argbFromHex(sourceHex);
  return {
    light: buildScheme(sourceArgb, false),
    dark: buildScheme(sourceArgb, true),
    success: customColor('#2e7d32', sourceArgb),
    warning: customColor('#f9a825', sourceArgb),
  };
}
