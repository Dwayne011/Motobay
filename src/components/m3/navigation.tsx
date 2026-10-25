import type { ReactNode } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import Badge from '@mui/material/Badge';
import ButtonBase from '@mui/material/ButtonBase';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { m3, m3Shape } from '../../theme/createM3Theme';
import { STATE, stateLayer } from '../../theme/elevation';

export interface NavDestination {
  to: string;
  label: string;
  icon: ReactNode;
  selectedIcon: ReactNode;
  /** Number for a large badge, true for a small dot badge. */
  badge?: number | boolean;
  /** Extra path prefixes that also count as this destination. */
  match?: string[];
}

export function useIsActive() {
  const { pathname } = useLocation();
  return (d: NavDestination) => {
    const paths = [d.to, ...(d.match ?? [])];
    return paths.some((p) => (p === '/' ? pathname === '/' : pathname === p || pathname.startsWith(`${p}/`)));
  };
}

function DestinationIcon({ d, active, indicatorWidth }: { d: NavDestination; active: boolean; indicatorWidth: number }) {
  const icon = active ? d.selectedIcon : d.icon;
  const badged =
    d.badge === true ? (
      <Badge variant="dot" overlap="circular">{icon}</Badge>
    ) : typeof d.badge === 'number' && d.badge > 0 ? (
      <Badge badgeContent={d.badge} max={999} overlap="circular">{icon}</Badge>
    ) : (
      icon
    );
  return (
    <Box
      className="m3-indicator"
      sx={{
        width: indicatorWidth,
        height: 32,
        borderRadius: m3Shape.full,
        display: 'grid',
        placeItems: 'center',
        transition: 'background-color 200ms',
        color: active ? m3('onSecondaryContainer') : m3('onSurfaceVariant'),
        backgroundColor: active ? m3('secondaryContainer') : 'transparent',
        '& .MuiSvgIcon-root': { fontSize: 24 },
      }}
    >
      {badged}
    </Box>
  );
}

function DestinationItem({ d, active, indicatorWidth }: { d: NavDestination; active: boolean; indicatorWidth: number }) {
  return (
    <ButtonBase
      component={RouterLink}
      to={d.to}
      aria-current={active ? 'page' : undefined}
      disableRipple
      sx={{
        flexDirection: 'column',
        gap: '4px',
        minWidth: 56,
        borderRadius: m3Shape.full,
        '&:hover .m3-indicator': {
          backgroundColor: active
            ? stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.hover)
            : `color-mix(in srgb, ${m3('onSurface')} ${STATE.hover}%, transparent)`,
        },
        '&.Mui-focusVisible .m3-indicator': {
          outline: `3px solid ${m3('secondary')}`,
          outlineOffset: 2,
        },
      }}
    >
      <DestinationIcon d={d} active={active} indicatorWidth={indicatorWidth} />
      <Typography
        variant="labelMedium"
        sx={{
          color: active ? m3('onSurface') : m3('onSurfaceVariant'),
          fontWeight: active ? 700 : 500,
        }}
      >
        {d.label}
      </Typography>
    </ButtonBase>
  );
}

/**
 * M3 Navigation rail, for medium and expanded windows (600dp+).
 * 80dp wide, 56×32 active indicator, optional FAB at the top.
 * https://m3.material.io/components/navigation-rail/specs
 */
export function NavigationRail({ destinations, fab }: { destinations: NavDestination[]; fab?: ReactNode }) {
  const isActive = useIsActive();
  return (
    <Box
      component="nav"
      aria-label="Main"
      sx={{
        width: 80,
        flexShrink: 0,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px',
        pt: '44px',
        backgroundColor: m3('surface'),
      }}
    >
      {fab && <Box sx={{ mb: '28px', mt: '-24px' }}>{fab}</Box>}
      {destinations.map((d) => (
        <DestinationItem key={d.to} d={d} active={isActive(d)} indicatorWidth={56} />
      ))}
    </Box>
  );
}

/**
 * M3 Navigation bar, for compact windows (under 600dp).
 * 80dp tall on surfaceContainer, 64×32 active indicator.
 * https://m3.material.io/components/navigation-bar/specs
 */
export function NavigationBar({ destinations }: { destinations: NavDestination[] }) {
  const isActive = useIsActive();
  return (
    <Box
      component="nav"
      aria-label="Main"
      sx={{
        height: 80,
        flexShrink: 0,
        display: 'flex',
        justifyContent: 'space-around',
        alignItems: 'flex-start',
        pt: '12px',
        pb: 'env(safe-area-inset-bottom)',
        boxSizing: 'content-box',
        backgroundColor: m3('surfaceContainer'),
      }}
    >
      {destinations.map((d) => (
        <Box key={d.to} sx={{ flex: 1, display: 'flex', justifyContent: 'center' }}>
          <DestinationItem d={d} active={isActive(d)} indicatorWidth={64} />
        </Box>
      ))}
    </Box>
  );
}
