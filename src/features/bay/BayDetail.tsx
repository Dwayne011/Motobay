import { useMemo, useState, type ReactElement } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import DirectionsIcon from '@mui/icons-material/Directions';
import EditLocationAltOutlinedIcon from '@mui/icons-material/EditLocationAltOutlined';
import MapIcon from '@mui/icons-material/Map';
import NavigationIcon from '@mui/icons-material/Navigation';
import ShareIcon from '@mui/icons-material/Share';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { BayFacts } from '../../components/BayChips';
import { notify } from '../../components/Notifier';
import { useUserLocation } from '../../hooks/useUserLocation';
import { favouritesStore, toggleFavourite } from '../../data/userStore';
import type { Bay } from '../../data/types';
import { m3 } from '../../theme/createM3Theme';
import { directionsUrl, distanceMetres, formatDistance, isAppleDevice, type MapsApp } from '../../utils/geo';
import { BayComments } from './BayComments';
import { BayPhotos } from './BayPhotos';

function DirectionsButton({ bay }: { bay: Bay }) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const apps: Array<{ app: MapsApp; label: string; icon: ReactElement }> = [
    { app: 'google', label: 'Google Maps', icon: <MapIcon /> },
    { app: 'apple', label: 'Apple Maps', icon: <MapIcon /> },
    { app: 'waze', label: 'Waze', icon: <NavigationIcon /> },
  ];
  // Put the platform's own maps app first.
  if (isAppleDevice()) apps.unshift(apps.splice(1, 1)[0]);
  const open = (app: MapsApp) => {
    window.open(directionsUrl(bay, app), '_blank', 'noopener');
    setAnchor(null);
  };
  return (
    <>
      <Button
        variant="filled"
        startIcon={<DirectionsIcon />}
        onClick={(e) => setAnchor(e.currentTarget)}
        aria-haspopup="menu"
        aria-expanded={anchor !== null}
      >
        Directions
      </Button>
      <Menu anchorEl={anchor} open={anchor !== null} onClose={() => setAnchor(null)}>
        {apps.map((a) => (
          <MenuItem key={a.app} onClick={() => open(a.app)}>
            <ListItemIcon>{a.icon}</ListItemIcon>
            {a.label}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}

/** Everything about one bay: facts, actions, community photos and comments. */
export function BayDetail({ bay }: { bay: Bay }) {
  const saved = favouritesStore.use((f) => Boolean(f[bay.id]));
  const { position } = useUserLocation();
  const distance = useMemo(() => (position ? distanceMetres(position, bay) : null), [position, bay]);
  const isSample = bay.id.startsWith('sample/');

  const share = async () => {
    const url = `${window.location.origin}/bay/${bay.id}`;
    try {
      if (navigator.share) await navigator.share({ title: bay.name, url });
      else {
        await navigator.clipboard.writeText(url);
        notify('Link copied');
      }
    } catch {
      // The user closed the share sheet.
    }
  };

  return (
    <Stack spacing={3} sx={{ p: 3 }}>
      <Box>
        <Typography variant="headlineSmall" component="h2">
          {bay.name}
        </Typography>
        <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant'), mt: 0.5 }}>
          {[bay.operator, distance !== null && `${formatDistance(distance)} away`].filter(Boolean).join(' · ') ||
            'Motorcycle parking'}
        </Typography>
      </Box>

      <BayFacts bay={bay} />

      <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
        <DirectionsButton bay={bay} />
        <Button
          variant="tonal"
          startIcon={saved ? <StarIcon /> : <StarBorderIcon />}
          onClick={() => {
            toggleFavourite(bay);
            notify(saved ? 'Removed from saved' : 'Saved');
          }}
          aria-pressed={saved}
        >
          {saved ? 'Saved' : 'Save'}
        </Button>
        <Tooltip title="Share">
          <IconButton onClick={share} aria-label="Share">
            <ShareIcon />
          </IconButton>
        </Tooltip>
      </Stack>

      <Divider />
      <BayPhotos bay={bay} />
      <Divider />
      <BayComments bay={bay} />
      <Divider />

      <Stack spacing={1}>
        <Typography variant="titleSmall" component="h3" sx={{ color: m3('onSurfaceVariant') }}>
          Data
        </Typography>
        <Typography variant="bodySmall" sx={{ color: m3('onSurfaceVariant') }}>
          {isSample
            ? 'This is a development sample, not a real bay.'
            : 'Bay data © OpenStreetMap contributors, available under the Open Database Licence. Something wrong? You can fix it on OpenStreetMap.'}
        </Typography>
        {!isSample && (
          <Box>
            <Button
              variant="text"
              startIcon={<EditLocationAltOutlinedIcon />}
              href={`https://www.openstreetmap.org/edit?${bay.id.replace('/', '=')}`}
              target="_blank"
              rel="noopener"
              sx={{ ml: -1.5 }}
            >
              Edit on OpenStreetMap
            </Button>
          </Box>
        )}
      </Stack>
    </Stack>
  );
}
