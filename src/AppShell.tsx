import { useMemo } from 'react';
import { Outlet } from 'react-router-dom';
import Box from '@mui/material/Box';
import ExploreIcon from '@mui/icons-material/Explore';
import ExploreOutlinedIcon from '@mui/icons-material/ExploreOutlined';
import RateReviewIcon from '@mui/icons-material/RateReview';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import SettingsIcon from '@mui/icons-material/Settings';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { NavigationBar, NavigationRail, type NavDestination } from './components/m3/navigation';
import { Notifier } from './components/Notifier';
import { communityStore } from './data/community';
import { profileStore } from './data/userStore';
import { useWindowSizeClass } from './hooks/useWindowSizeClass';

/**
 * Adaptive navigation per M3 window size classes:
 * compact → bottom Navigation bar; medium and expanded → Navigation rail.
 */
export function AppShell() {
  const size = useWindowSizeClass();
  const moderator = profileStore.use((p) => p.moderator);
  const photos = communityStore.use((s) => s.photos);
  const comments = communityStore.use((s) => s.comments);
  const pending = useMemo(
    () => photos.filter((p) => p.status === 'pending').length + comments.filter((c) => c.status === 'pending').length,
    [photos, comments],
  );

  const destinations: NavDestination[] = [
    { to: '/', label: 'Explore', icon: <ExploreOutlinedIcon />, selectedIcon: <ExploreIcon />, match: ['/bay'] },
    { to: '/saved', label: 'Saved', icon: <StarBorderIcon />, selectedIcon: <StarIcon /> },
    ...(moderator
      ? [{ to: '/review', label: 'Review', icon: <RateReviewOutlinedIcon />, selectedIcon: <RateReviewIcon />, badge: pending }]
      : []),
    { to: '/settings', label: 'Settings', icon: <SettingsOutlinedIcon />, selectedIcon: <SettingsIcon /> },
  ];

  const compact = size === 'compact';
  return (
    <Box sx={{ height: '100dvh', display: 'flex', flexDirection: compact ? 'column' : 'row', pt: 'env(safe-area-inset-top)' }}>
      {!compact && <NavigationRail destinations={destinations} />}
      <Box component="main" sx={{ flex: 1, minHeight: 0, minWidth: 0 }}>
        <Outlet />
      </Box>
      {compact && <NavigationBar destinations={destinations} />}
      <Notifier />
    </Box>
  );
}
