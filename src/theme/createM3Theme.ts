import { createTheme, type Theme } from '@mui/material/styles';
import { generateSchemes, type M3CustomColor, type M3Scheme } from './m3Scheme';
import { floatingShadow, muiElevationToM3, STATE, stateLayer, tonalSurface } from './elevation';
import { legacyVariantAliases, m3TypeScale, m3VariantMapping } from './typography';

export type M3PaletteTokens = M3Scheme & {
  success: string;
  onSuccess: string;
  successContainer: string;
  onSuccessContainer: string;
  warning: string;
  onWarning: string;
  warningContainer: string;
  onWarningContainer: string;
};

/** M3 shape scale. https://m3.material.io/styles/shape/corner-radius-scale */
export const m3Shape = {
  none: 0,
  extraSmall: 4,
  small: 8,
  medium: 12,
  large: 16,
  extraLarge: 28,
  full: 9999,
} as const;

/** Shorthand for an M3 colour role as a CSS variable, so it follows light/dark. */
export const m3 = (role: keyof M3PaletteTokens) => `var(--mui-palette-m3-${role})`;

const disabledContainer = `color-mix(in srgb, ${m3('onSurface')} 12%, transparent)`;
const disabledContent = `color-mix(in srgb, ${m3('onSurface')} 38%, transparent)`;
const transparentLayer = (on: string, pct: number) => `color-mix(in srgb, ${on} ${pct}%, transparent)`;

function tokens(scheme: M3Scheme, success: M3CustomColor, warning: M3CustomColor): M3PaletteTokens {
  return {
    ...scheme,
    success: success.color,
    onSuccess: success.onColor,
    successContainer: success.container,
    onSuccessContainer: success.onContainer,
    warning: warning.color,
    onWarning: warning.onColor,
    warningContainer: warning.container,
    onWarningContainer: warning.onContainer,
  };
}

function muiPalette(t: M3PaletteTokens) {
  return {
    m3: t,
    primary: { main: t.primary, contrastText: t.onPrimary },
    secondary: { main: t.secondary, contrastText: t.onSecondary },
    error: { main: t.error, contrastText: t.onError },
    info: { main: t.tertiary, contrastText: t.onTertiary },
    success: { main: t.success, contrastText: t.onSuccess },
    warning: { main: t.warning, contrastText: t.onWarning },
    background: { default: t.surface, paper: t.surfaceContainerLow },
    text: { primary: t.onSurface, secondary: t.onSurfaceVariant, disabled: disabledContent },
    divider: t.outlineVariant,
  };
}

/**
 * Builds the MUI v6 theme from one source colour. Uses MUI v6 CSS theme
 * variables with light and dark colour schemes, so switching mode swaps CSS
 * variables without re-rendering the tree.
 */
export function createM3Theme(sourceHex: string): Theme {
  const s = generateSchemes(sourceHex);
  const light = tokens(s.light, s.success.light, s.warning.light);
  const dark = tokens(s.dark, s.success.dark, s.warning.dark);

  return createTheme({
    cssVariables: { colorSchemeSelector: 'class' },
    colorSchemes: {
      light: { palette: muiPalette(light) },
      dark: { palette: muiPalette(dark) },
    },
    // M3 window size classes: compact < 600, medium 600–839, expanded 840–1199, large 1200+.
    breakpoints: { values: { xs: 0, sm: 600, md: 840, lg: 1200, xl: 1600 } },
    shape: { borderRadius: m3Shape.extraSmall },
    // Depth is tonal. Drop shadows are removed from the elevation system.
    shadows: Array(25).fill('none') as Theme['shadows'],
    typography: {
      fontFamily: m3TypeScale.bodyLarge.fontFamily as string,
      ...legacyVariantAliases,
      ...m3TypeScale,
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            backgroundColor: m3('surface'),
            color: m3('onSurface'),
            ...m3TypeScale.bodyLarge,
          },
        },
      },
      MuiTypography: {
        defaultProps: { variant: 'bodyLarge', variantMapping: m3VariantMapping },
      },

      // Surfaces: tonal elevation instead of shadows.
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: ({ ownerState }) => ({
            backgroundImage: 'none',
            boxShadow: 'none',
            ...(ownerState.variant === 'elevation' && {
              backgroundColor: tonalSurface(muiElevationToM3(ownerState.elevation ?? 0)),
            }),
            ...(ownerState.variant === 'outlined' && {
              backgroundColor: m3('surface'),
              borderColor: m3('outlineVariant'),
            }),
            ...(ownerState.variant === 'filled' && {
              backgroundColor: m3('surfaceContainerHighest'),
            }),
          }),
        },
      },
      MuiCard: {
        defaultProps: { elevation: 1 },
        styleOverrides: { root: { borderRadius: m3Shape.medium } },
      },
      MuiAppBar: {
        defaultProps: { elevation: 0, color: 'inherit' },
        styleOverrides: {
          root: { backgroundColor: m3('surface'), color: m3('onSurface') },
        },
      },
      MuiToolbar: {
        styleOverrides: { root: { minHeight: 64, gap: 4, '@media (min-width:0px)': { minHeight: 64 } } },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            borderRadius: m3Shape.extraLarge,
            backgroundColor: m3('surfaceContainerHigh'),
            minWidth: 280,
            maxWidth: 560,
          },
        },
      },
      MuiDialogTitle: {
        styleOverrides: { root: { ...m3TypeScale.headlineSmall, padding: '24px 24px 16px' } },
      },
      MuiDialogContent: {
        styleOverrides: { root: { ...m3TypeScale.bodyMedium, color: m3('onSurfaceVariant'), padding: '0 24px' } },
      },
      MuiDialogActions: {
        styleOverrides: { root: { padding: 24, gap: 8, '& > :not(style) ~ :not(style)': { marginLeft: 0 } } },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: { backgroundColor: m3('surfaceContainerLow'), border: 'none' },
          paperAnchorBottom: {
            borderTopLeftRadius: m3Shape.extraLarge,
            borderTopRightRadius: m3Shape.extraLarge,
          },
        },
      },
      MuiMenu: {
        styleOverrides: {
          paper: {
            backgroundColor: m3('surfaceContainer'),
            borderRadius: m3Shape.extraSmall,
            boxShadow: floatingShadow,
          },
          list: { paddingTop: 8, paddingBottom: 8 },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: {
            ...m3TypeScale.labelLarge,
            minHeight: 48,
            gap: 12,
            '&:hover': { backgroundColor: transparentLayer(m3('onSurface'), STATE.hover) },
          },
        },
      },

      // Common buttons. https://m3.material.io/components/buttons/specs
      MuiButton: {
        defaultProps: { variant: 'filled', disableElevation: true },
        styleOverrides: {
          root: {
            ...m3TypeScale.labelLarge,
            height: 40,
            minWidth: 48,
            padding: '0 24px',
            borderRadius: m3Shape.full,
            textTransform: 'none',
            '&.Mui-disabled': { color: disabledContent },
          },
          startIcon: { marginLeft: -8, marginRight: 8, '& > *:nth-of-type(1)': { fontSize: 18 } },
          endIcon: { marginLeft: 8, marginRight: -8, '& > *:nth-of-type(1)': { fontSize: 18 } },
        },
        variants: [
          {
            props: { variant: 'filled' },
            style: {
              backgroundColor: m3('primary'),
              color: m3('onPrimary'),
              '&:hover': { backgroundColor: stateLayer(m3('primary'), m3('onPrimary'), STATE.hover) },
              '&:active, &.Mui-focusVisible': {
                backgroundColor: stateLayer(m3('primary'), m3('onPrimary'), STATE.pressed),
              },
              '&.Mui-disabled': { backgroundColor: disabledContainer },
            },
          },
          {
            props: { variant: 'tonal' },
            style: {
              backgroundColor: m3('secondaryContainer'),
              color: m3('onSecondaryContainer'),
              '&:hover': {
                backgroundColor: stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.hover),
              },
              '&:active, &.Mui-focusVisible': {
                backgroundColor: stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.pressed),
              },
              '&.Mui-disabled': { backgroundColor: disabledContainer },
            },
          },
          {
            props: { variant: 'elevated' },
            style: {
              backgroundColor: tonalSurface(1),
              color: m3('primary'),
              '&:hover': { backgroundColor: stateLayer(tonalSurface(2), m3('primary'), STATE.hover) },
              '&.Mui-disabled': { backgroundColor: disabledContainer },
            },
          },
          {
            props: { variant: 'outlined' },
            style: {
              border: `1px solid ${m3('outline')}`,
              color: m3('primary'),
              '&:hover': { border: `1px solid ${m3('outline')}`, backgroundColor: transparentLayer(m3('primary'), STATE.hover) },
              '&:active, &.Mui-focusVisible': { backgroundColor: transparentLayer(m3('primary'), STATE.pressed) },
              '&.Mui-disabled': { border: `1px solid ${disabledContainer}` },
            },
          },
          {
            props: { variant: 'text' },
            style: {
              padding: '0 12px',
              color: m3('primary'),
              '&:hover': { backgroundColor: transparentLayer(m3('primary'), STATE.hover) },
              '&:active, &.Mui-focusVisible': { backgroundColor: transparentLayer(m3('primary'), STATE.pressed) },
            },
          },
        ],
      },
      MuiIconButton: {
        styleOverrides: {
          root: {
            width: 40,
            height: 40,
            color: m3('onSurfaceVariant'),
            '&:hover': { backgroundColor: transparentLayer(m3('onSurfaceVariant'), STATE.hover) },
          },
        },
      },
      // FAB. https://m3.material.io/components/floating-action-button/specs
      MuiFab: {
        defaultProps: { size: 'large' },
        styleOverrides: {
          root: {
            ...m3TypeScale.labelLarge,
            textTransform: 'none',
            width: 56,
            height: 56,
            borderRadius: m3Shape.large,
            backgroundColor: m3('primaryContainer'),
            color: m3('onPrimaryContainer'),
            boxShadow: floatingShadow,
            '&:hover': { backgroundColor: stateLayer(m3('primaryContainer'), m3('onPrimaryContainer'), STATE.hover) },
            '&:active': { boxShadow: floatingShadow },
          },
          sizeSmall: { width: 40, height: 40, minHeight: 40, borderRadius: m3Shape.medium },
          extended: { width: 'auto', height: 56, padding: '0 20px 0 16px', gap: 12, borderRadius: m3Shape.large },
        },
      },

      // Chips. https://m3.material.io/components/chips/specs
      MuiChip: {
        styleOverrides: {
          root: {
            ...m3TypeScale.labelLarge,
            height: 32,
            borderRadius: m3Shape.small,
          },
          outlined: {
            borderColor: m3('outlineVariant'),
            color: m3('onSurfaceVariant'),
            '&.MuiChip-clickable:hover': { backgroundColor: transparentLayer(m3('onSurfaceVariant'), STATE.hover) },
          },
          filled: {
            backgroundColor: m3('secondaryContainer'),
            color: m3('onSecondaryContainer'),
            '&.MuiChip-clickable:hover': {
              backgroundColor: stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.hover),
            },
          },
          icon: { fontSize: 18, color: 'inherit', marginLeft: 8 },
          label: { paddingLeft: 12, paddingRight: 12 },
        },
      },

      // Text fields (outlined). https://m3.material.io/components/text-fields/specs
      MuiTextField: { defaultProps: { variant: 'outlined' } },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: m3Shape.extraSmall,
            ...m3TypeScale.bodyLarge,
            '& .MuiOutlinedInput-notchedOutline': { borderColor: m3('outline') },
            '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: m3('onSurface') },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: m3('primary'), borderWidth: 2 },
          },
        },
      },
      MuiInputLabel: {
        styleOverrides: {
          root: { color: m3('onSurfaceVariant'), '&.Mui-focused': { color: m3('primary') } },
        },
      },

      // Switch. https://m3.material.io/components/switch/specs
      MuiSwitch: {
        styleOverrides: {
          root: { width: 52, height: 32, padding: 0, overflow: 'visible' },
          switchBase: {
            padding: 0,
            margin: 0,
            width: 32,
            height: 32,
            '&:hover': { backgroundColor: transparentLayer(m3('onSurface'), STATE.hover) },
            '&.Mui-checked': {
              transform: 'translateX(20px)',
              '&:hover': { backgroundColor: transparentLayer(m3('primary'), STATE.hover) },
              '& + .MuiSwitch-track': { backgroundColor: m3('primary'), borderColor: m3('primary'), opacity: 1 },
            },
            '&.Mui-disabled + .MuiSwitch-track': { opacity: 0.12 },
          },
          thumb: {
            width: 16,
            height: 16,
            boxShadow: 'none',
            backgroundColor: m3('outline'),
            transition: 'width 150ms, height 150ms',
            '.Mui-checked &': { width: 24, height: 24, backgroundColor: m3('onPrimary') },
          },
          track: {
            boxSizing: 'border-box',
            borderRadius: m3Shape.full,
            opacity: 1,
            backgroundColor: m3('surfaceContainerHighest'),
            border: `2px solid ${m3('outline')}`,
          },
        },
      },

      MuiListItemButton: {
        styleOverrides: {
          root: {
            '&:hover': { backgroundColor: transparentLayer(m3('onSurface'), STATE.hover) },
            '&.Mui-selected': {
              backgroundColor: m3('secondaryContainer'),
              color: m3('onSecondaryContainer'),
              '&:hover': {
                backgroundColor: stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.hover),
              },
            },
          },
        },
      },
      MuiListItemText: {
        styleOverrides: {
          primary: { ...m3TypeScale.bodyLarge, color: m3('onSurface') },
          secondary: { ...m3TypeScale.bodyMedium, color: m3('onSurfaceVariant') },
        },
      },
      MuiListItemIcon: {
        styleOverrides: { root: { color: m3('onSurfaceVariant'), minWidth: 40 } },
      },
      MuiListSubheader: {
        styleOverrides: {
          root: { ...m3TypeScale.titleSmall, color: m3('onSurfaceVariant'), backgroundColor: 'transparent' },
        },
      },
      MuiDivider: { styleOverrides: { root: { borderColor: m3('outlineVariant') } } },
      MuiBadge: {
        styleOverrides: {
          badge: {
            ...m3TypeScale.labelSmall,
            backgroundColor: m3('error'),
            color: m3('onError'),
            height: 16,
            minWidth: 16,
            padding: '0 4px',
            borderRadius: m3Shape.full,
          },
          dot: { height: 6, minWidth: 6, padding: 0 },
        },
      },
      MuiTooltip: {
        styleOverrides: {
          tooltip: {
            ...m3TypeScale.bodySmall,
            backgroundColor: m3('inverseSurface'),
            color: m3('inverseOnSurface'),
            borderRadius: m3Shape.extraSmall,
          },
        },
      },
      MuiSnackbarContent: {
        styleOverrides: {
          root: {
            ...m3TypeScale.bodyMedium,
            backgroundColor: m3('inverseSurface'),
            color: m3('inverseOnSurface'),
            borderRadius: m3Shape.extraSmall,
            boxShadow: floatingShadow,
            '& .MuiButton-root': { color: m3('inversePrimary') },
          },
        },
      },
      MuiLinearProgress: {
        styleOverrides: {
          root: { height: 4, borderRadius: m3Shape.full, backgroundColor: m3('surfaceContainerHighest') },
          bar: { borderRadius: m3Shape.full, backgroundColor: m3('primary') },
        },
      },
      MuiCircularProgress: { styleOverrides: { root: { color: m3('primary') } } },
      MuiSkeleton: { styleOverrides: { root: { backgroundColor: m3('surfaceContainerHighest') } } },
    },
  });
}
