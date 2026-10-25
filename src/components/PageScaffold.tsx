import { useState, type ReactNode } from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import useScrollTrigger from '@mui/material/useScrollTrigger';
import { m3 } from '../theme/createM3Theme';

/**
 * Page with an M3 small top app bar. The bar is on surface and changes to
 * surfaceContainer once content scrolls under it (M3 "on scroll" state).
 */
export function PageScaffold({ title, actions, children }: { title: string; actions?: ReactNode; children: ReactNode }) {
  const [scroller, setScroller] = useState<HTMLElement | null>(null);
  const scrolled = useScrollTrigger({ target: scroller ?? undefined, disableHysteresis: true, threshold: 0 });
  return (
    <Box ref={setScroller} sx={{ height: '100%', overflow: 'auto' }}>
      <AppBar
        position="sticky"
        sx={{ backgroundColor: scrolled ? m3('surfaceContainer') : m3('surface'), transition: 'background-color 200ms' }}
      >
        <Toolbar>
          <Typography variant="titleLarge" component="h1" sx={{ flex: 1 }}>
            {title}
          </Typography>
          {actions}
        </Toolbar>
      </AppBar>
      <Box sx={{ maxWidth: 840, mx: 'auto', px: { xs: 2, sm: 3 }, pb: 4 }}>{children}</Box>
    </Box>
  );
}
