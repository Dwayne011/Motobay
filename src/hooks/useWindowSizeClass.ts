import useMediaQuery from '@mui/material/useMediaQuery';
import { useTheme } from '@mui/material/styles';

/**
 * M3 window size classes.
 * https://m3.material.io/foundations/layout/applying-layout/window-size-classes
 */
export type WindowSizeClass = 'compact' | 'medium' | 'expanded';

export function useWindowSizeClass(): WindowSizeClass {
  const theme = useTheme();
  const medium = useMediaQuery(theme.breakpoints.up('sm'), { noSsr: true });
  const expanded = useMediaQuery(theme.breakpoints.up('md'), { noSsr: true });
  if (expanded) return 'expanded';
  if (medium) return 'medium';
  return 'compact';
}
