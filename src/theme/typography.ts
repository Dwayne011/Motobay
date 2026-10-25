import type { TypographyStyle } from '@mui/material/styles';

/**
 * M3 type scale: Display, Headline, Title, Body and Label, each in
 * Large / Medium / Small. Values come from the M3 baseline tokens.
 * https://m3.material.io/styles/typography/type-scale-tokens
 */
const FONT = '"Roboto", "Helvetica Neue", Arial, sans-serif';

function style(
  size: number,
  lineHeight: number,
  weight: number,
  tracking: number,
): TypographyStyle {
  return {
    fontFamily: FONT,
    fontSize: `${size / 16}rem`,
    lineHeight: `${lineHeight / 16}rem`,
    fontWeight: weight,
    letterSpacing: `${tracking / 16}rem`,
  };
}

export const m3TypeScale = {
  displayLarge: style(57, 64, 400, -0.25),
  displayMedium: style(45, 52, 400, 0),
  displaySmall: style(36, 44, 400, 0),
  headlineLarge: style(32, 40, 400, 0),
  headlineMedium: style(28, 36, 400, 0),
  headlineSmall: style(24, 32, 400, 0),
  titleLarge: style(22, 28, 400, 0),
  titleMedium: style(16, 24, 500, 0.15),
  titleSmall: style(14, 20, 500, 0.1),
  bodyLarge: style(16, 24, 400, 0.5),
  bodyMedium: style(14, 20, 400, 0.25),
  bodySmall: style(12, 16, 400, 0.4),
  labelLarge: style(14, 20, 500, 0.1),
  labelMedium: style(12, 16, 500, 0.5),
  labelSmall: style(11, 16, 500, 0.5),
} as const;

export type M3TypeVariant = keyof typeof m3TypeScale;

/** HTML element each M3 variant renders as by default. */
export const m3VariantMapping: Record<M3TypeVariant, string> = {
  displayLarge: 'h1',
  displayMedium: 'h1',
  displaySmall: 'h1',
  headlineLarge: 'h2',
  headlineMedium: 'h2',
  headlineSmall: 'h2',
  titleLarge: 'h3',
  titleMedium: 'h4',
  titleSmall: 'h5',
  bodyLarge: 'p',
  bodyMedium: 'p',
  bodySmall: 'p',
  labelLarge: 'span',
  labelMedium: 'span',
  labelSmall: 'span',
};

/**
 * MUI's built-in components still read the legacy MD2 variants (body1,
 * button, caption, ...). Pointing them at M3 styles keeps those components
 * on the M3 scale. App code can't use the legacy names (see augment.d.ts).
 */
export const legacyVariantAliases = {
  h1: m3TypeScale.displayLarge,
  h2: m3TypeScale.displayMedium,
  h3: m3TypeScale.displaySmall,
  h4: m3TypeScale.headlineMedium,
  h5: m3TypeScale.headlineSmall,
  h6: m3TypeScale.titleLarge,
  subtitle1: m3TypeScale.titleMedium,
  subtitle2: m3TypeScale.titleSmall,
  body1: m3TypeScale.bodyLarge,
  body2: m3TypeScale.bodyMedium,
  button: { ...m3TypeScale.labelLarge, textTransform: 'none' as const },
  caption: m3TypeScale.bodySmall,
  overline: { ...m3TypeScale.labelSmall, textTransform: 'none' as const },
};
