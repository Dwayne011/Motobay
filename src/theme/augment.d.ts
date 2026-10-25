import type { TypographyStyle } from '@mui/material/styles';
import type { M3PaletteTokens } from './createM3Theme';

type CSSProperties = TypographyStyle;

type M3Variants = {
  displayLarge: CSSProperties;
  displayMedium: CSSProperties;
  displaySmall: CSSProperties;
  headlineLarge: CSSProperties;
  headlineMedium: CSSProperties;
  headlineSmall: CSSProperties;
  titleLarge: CSSProperties;
  titleMedium: CSSProperties;
  titleSmall: CSSProperties;
  bodyLarge: CSSProperties;
  bodyMedium: CSSProperties;
  bodySmall: CSSProperties;
  labelLarge: CSSProperties;
  labelMedium: CSSProperties;
  labelSmall: CSSProperties;
};

declare module '@mui/material/styles' {
  interface Palette {
    m3: M3PaletteTokens;
  }
  interface PaletteOptions {
    m3?: M3PaletteTokens;
  }
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface TypographyVariants extends M3Variants {}
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface TypographyVariantsOptions extends Partial<M3Variants> {}
  interface BreakpointOverrides {
    xs: true;
    sm: true;
    md: true;
    lg: true;
    xl: true;
  }
}

// Only the M3 type scale is available to app code; the MD2 names are switched off.
declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    displayLarge: true;
    displayMedium: true;
    displaySmall: true;
    headlineLarge: true;
    headlineMedium: true;
    headlineSmall: true;
    titleLarge: true;
    titleMedium: true;
    titleSmall: true;
    bodyLarge: true;
    bodyMedium: true;
    bodySmall: true;
    labelLarge: true;
    labelMedium: true;
    labelSmall: true;
    h1: false;
    h2: false;
    h3: false;
    h4: false;
    h5: false;
    h6: false;
    subtitle1: false;
    subtitle2: false;
    body1: false;
    body2: false;
    caption: false;
    overline: false;
    button: false;
  }
}

// M3 common buttons: filled, tonal, elevated, outlined and text. MD2 "contained" is removed.
declare module '@mui/material/Button' {
  interface ButtonPropsVariantOverrides {
    filled: true;
    tonal: true;
    elevated: true;
    contained: false;
  }
}

// M3 cards come in elevated, filled and outlined styles.
declare module '@mui/material/Paper' {
  interface PaperPropsVariantOverrides {
    filled: true;
  }
}
