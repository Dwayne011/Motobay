import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { BayList } from '../../components/BayList';
import { PageScaffold } from '../../components/PageScaffold';
import { favouritesStore } from '../../data/userStore';
import { useUserLocation } from '../../hooks/useUserLocation';
import { m3 } from '../../theme/createM3Theme';
import { distanceMetres } from '../../utils/geo';

export function SavedPage() {
  const favourites = favouritesStore.use((f) => f);
  const { position } = useUserLocation();
  const navigate = useNavigate();

  const items = useMemo(
    () =>
      Object.values(favourites)
        .map((bay) => ({ bay, distance: position ? distanceMetres(position, bay) : null }))
        .sort((a, b) => (a.distance ?? 0) - (b.distance ?? 0) || a.bay.name.localeCompare(b.bay.name)),
    [favourites, position],
  );

  return (
    <PageScaffold title="Saved">
      {items.length === 0 ? (
        <Stack alignItems="center" spacing={2} sx={{ py: 8, textAlign: 'center' }}>
          <StarBorderIcon sx={{ fontSize: 48, color: m3('onSurfaceVariant') }} />
          <Typography variant="titleMedium">No saved bays yet</Typography>
          <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant'), maxWidth: 320 }}>
            Save the bays you use most so you can get directions in one tap.
          </Typography>
          <Button variant="tonal" onClick={() => navigate('/')}>
            Find bays
          </Button>
        </Stack>
      ) : (
        <BayList items={items} onSelect={(b) => navigate(`/bay/${b.id}`)} />
      )}
    </PageScaffold>
  );
}
