import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Fab from '@mui/material/Fab';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckIcon from '@mui/icons-material/Check';
import FormatListBulletedIcon from '@mui/icons-material/FormatListBulleted';
import MapOutlinedIcon from '@mui/icons-material/MapOutlined';
import MyLocationIcon from '@mui/icons-material/MyLocation';
import { MIN_BAY_ZOOM } from '../../config';
import { BayList, type BayWithDistance } from '../../components/BayList';
import { notify } from '../../components/Notifier';
import { SearchBar } from '../../components/m3/SearchBar';
import { SegmentedButton } from '../../components/m3/SegmentedButton';
import { bayStore, loadBay } from '../../data/bayStore';
import { searchPlace } from '../../data/geocode';
import type { Bay } from '../../data/types';
import { useUserLocation } from '../../hooks/useUserLocation';
import { useWindowSizeClass } from '../../hooks/useWindowSizeClass';
import { m3, m3Shape } from '../../theme/createM3Theme';
import { distanceMetres } from '../../utils/geo';
import { BayDetail } from '../bay/BayDetail';
import { BayMap, type MapCommand, type MapView } from './BayMap';

type PriceFilter = 'all' | 'free' | 'paid';

function useRouteBay(id: string | undefined) {
  const cached = bayStore.use((s) => (id ? s.bays[id] : undefined));
  const [missing, setMissing] = useState(false);
  useEffect(() => {
    setMissing(false);
    if (id && !cached) loadBay(id).then((b) => !b && setMissing(true), () => setMissing(true));
  }, [id, cached]);
  return { bay: cached ?? null, missing };
}

export function ExplorePage() {
  const size = useWindowSizeClass();
  const navigate = useNavigate();
  const { type, num } = useParams();
  const bayId = type && num ? `${type}/${num}` : undefined;
  const { bay: selectedBay, missing } = useRouteBay(bayId);

  const allBays = bayStore.use((s) => s.bays);
  const loading = bayStore.use((s) => s.loading);
  const loadError = bayStore.use((s) => s.error);
  const { position, locating, error: locationError, locate } = useUserLocation();

  const [view, setView] = useState<MapView | null>(null);
  const [price, setPrice] = useState<PriceFilter>('all');
  const [coveredOnly, setCoveredOnly] = useState(false);
  const [compactMode, setCompactMode] = useState<'map' | 'list'>('map');
  const [command, setCommand] = useState<MapCommand | null>(null);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    if (loadError) notify(`Couldn't load bays: ${loadError}`);
  }, [loadError]);
  useEffect(() => {
    if (locationError) notify(locationError);
  }, [locationError]);

  // Centre on a bay opened from a link or the list.
  useEffect(() => {
    if (selectedBay) setCommand({ kind: 'fly', lat: selectedBay.lat, lon: selectedBay.lon });
  }, [selectedBay]);

  const visible: BayWithDistance[] = useMemo(() => {
    if (!view) return [];
    const { bbox } = view;
    const origin = position ?? view.center;
    return Object.values(allBays)
      .filter((b) => b.lat >= bbox.south && b.lat <= bbox.north && b.lon >= bbox.west && b.lon <= bbox.east)
      .filter((b) => price === 'all' || b.fee === price)
      .filter((b) => !coveredOnly || b.covered === true)
      .map((bay) => ({ bay, distance: distanceMetres(origin, bay) }))
      .sort((a, b) => a.distance - b.distance)
      .map((x) => ({ ...x, distance: position ? x.distance : null }));
  }, [allBays, view, price, coveredOnly, position]);

  const mapBays = useMemo(() => {
    const list = visible.map((v) => v.bay);
    if (selectedBay && !list.some((b) => b.id === selectedBay.id)) list.push(selectedBay);
    return list;
  }, [visible, selectedBay]);

  const select = useCallback((bay: Bay) => navigate(`/bay/${bay.id}`), [navigate]);
  const closeDetail = () => navigate('/');

  const search = async (q: string) => {
    setSearching(true);
    try {
      const place = await searchPlace(q);
      if (place) setCommand({ kind: 'fit', bbox: place.bbox });
      else notify(`No UK place found for "${q}"`);
    } catch {
      notify('Search is unavailable right now');
    } finally {
      setSearching(false);
    }
  };

  const goToMe = async () => {
    const p = await locate();
    if (p) setCommand({ kind: 'fly', lat: p.lat, lon: p.lon, zoom: 16 });
  };

  const zoomedOut = !view || view.zoom < MIN_BAY_ZOOM;

  const filters = (
    <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
      <SegmentedButton<PriceFilter>
        aria-label="Price"
        value={price}
        onChange={setPrice}
        segments={[
          { value: 'all', label: 'All' },
          { value: 'free', label: 'Free' },
          { value: 'paid', label: 'Paid' },
        ]}
      />
      <Chip
        label="Covered"
        variant={coveredOnly ? 'filled' : 'outlined'}
        icon={coveredOnly ? <CheckIcon /> : undefined}
        onClick={() => setCoveredOnly((v) => !v)}
        aria-pressed={coveredOnly}
        sx={{ backgroundColor: coveredOnly ? undefined : m3('surface') }}
      />
    </Stack>
  );

  const searchBar = (floating: boolean) => (
    <SearchBar placeholder="Search a UK town or postcode" onSubmit={search} loading={searching} floating={floating} />
  );

  const listContent = (
    <>
      {zoomedOut ? (
        <EmptyHint text="Zoom in or search for a place to see motorcycle bays." />
      ) : visible.length === 0 && !loading ? (
        <EmptyHint text="No bays match here. Try another area or clear the filters." />
      ) : (
        <BayList items={visible} selectedId={bayId} onSelect={select} />
      )}
    </>
  );

  const map = (
    <Box sx={{ position: 'relative', height: '100%', width: '100%' }}>
      <BayMap
        bays={mapBays}
        selectedId={bayId ?? null}
        userPosition={position}
        command={command}
        onSelect={select}
        onViewChange={setView}
      />
      {loading && <LinearProgress sx={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 500, borderRadius: 0 }} />}
      <Fab
        aria-label="Show my location"
        onClick={goToMe}
        sx={{ position: 'absolute', right: 16, bottom: 16, zIndex: 500 }}
      >
        {locating ? <CircularProgress size={24} sx={{ color: 'inherit' }} /> : <MyLocationIcon />}
      </Fab>
    </Box>
  );

  const detail = selectedBay ? (
    <BayDetail bay={selectedBay} />
  ) : missing ? (
    <EmptyHint text="This bay couldn't be found. It may have been removed from the map." />
  ) : (
    <Box sx={{ p: 4, display: 'grid', placeItems: 'center' }}>
      <CircularProgress />
    </Box>
  );

  // Compact and medium: one pane at a time. Full-screen detail, or a
  // full-bleed map with floating controls (M3 recommends a single pane below 840dp).
  if (size !== 'expanded') {
    if (bayId) {
      return (
        <Box sx={{ height: '100%', overflow: 'auto' }}>
          <AppBar position="sticky">
            <Toolbar>
              <IconButton edge="start" aria-label="Back" onClick={closeDetail} sx={{ color: m3('onSurface') }}>
                <ArrowBackIcon />
              </IconButton>
              <Typography variant="titleLarge" component="h1" noWrap>
                Bay details
              </Typography>
            </Toolbar>
          </AppBar>
          <Box sx={{ maxWidth: 720, mx: 'auto' }}>{detail}</Box>
        </Box>
      );
    }
    return (
      <Box sx={{ position: 'relative', height: '100%' }}>
        <Box sx={{ position: 'absolute', inset: 0, visibility: compactMode === 'map' ? 'visible' : 'hidden' }}>{map}</Box>
        {compactMode === 'list' && (
          <Box sx={{ position: 'absolute', inset: 0, pt: '136px', pb: '72px', overflow: 'auto', backgroundColor: m3('surface') }}>
            <Box sx={{ maxWidth: 720, mx: 'auto' }}>{listContent}</Box>
          </Box>
        )}
        <Stack spacing={1} sx={{ position: 'absolute', top: 0, left: 0, right: 0, p: 2, zIndex: 600, maxWidth: 560 }}>
          {searchBar(compactMode === 'map')}
          {filters}
        </Stack>
        <Box sx={{ position: 'absolute', left: '50%', transform: 'translateX(-50%)', bottom: 16, zIndex: 600 }}>
          <Paper elevation={3} sx={{ borderRadius: `${m3Shape.full}px` }}>
            <SegmentedButton
              aria-label="View"
              value={compactMode}
              onChange={setCompactMode}
              segments={[
                { value: 'map', label: 'Map', icon: <MapOutlinedIcon /> },
                { value: 'list', label: `List${zoomedOut ? '' : ` (${visible.length})`}`, icon: <FormatListBulletedIcon /> },
              ]}
            />
          </Paper>
        </Box>
      </Box>
    );
  }

  // Expanded: M3 list-detail canonical layout, with the map as the supporting pane.
  const paneWidth = 400;
  return (
    <Box sx={{ display: 'flex', height: '100%', gap: 3, p: 3, pl: 0, boxSizing: 'border-box' }}>
      <Paper
        elevation={1}
        sx={{ width: paneWidth, flexShrink: 0, borderRadius: `${m3Shape.extraLarge}px`, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
      >
        {bayId ? (
          <>
            <Toolbar sx={{ minHeight: 64 }}>
              <IconButton edge="start" aria-label="Back to list" onClick={closeDetail}>
                <ArrowBackIcon />
              </IconButton>
              <Typography variant="titleLarge" component="h1">
                Bay details
              </Typography>
            </Toolbar>
            <Box sx={{ overflow: 'auto', flex: 1 }}>{detail}</Box>
          </>
        ) : (
          <>
            <Stack spacing={2} sx={{ p: 2 }}>
              {searchBar(false)}
              {filters}
              <Typography variant="labelLarge" sx={{ color: m3('onSurfaceVariant'), px: 1 }} aria-live="polite">
                {zoomedOut ? 'Nearby bays' : `${visible.length} ${visible.length === 1 ? 'bay' : 'bays'} in view`}
              </Typography>
            </Stack>
            <Box sx={{ overflow: 'auto', flex: 1, pb: 1 }}>{listContent}</Box>
          </>
        )}
      </Paper>
      <Box sx={{ flex: 1, borderRadius: `${m3Shape.extraLarge}px`, overflow: 'hidden', minWidth: 0 }}>{map}</Box>
    </Box>
  );
}

function EmptyHint({ text }: { text: string }) {
  return (
    <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant'), p: 3, textAlign: 'center' }}>
      {text}
    </Typography>
  );
}
