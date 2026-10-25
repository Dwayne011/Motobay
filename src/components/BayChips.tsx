import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import HourglassBottomIcon from '@mui/icons-material/HourglassBottom';
import RoofingIcon from '@mui/icons-material/Roofing';
import TwoWheelerIcon from '@mui/icons-material/TwoWheeler';
import type { Bay, Fee } from '../data/types';
import { m3 } from '../theme/createM3Theme';

export const FEE_LABEL: Record<Fee, string> = { free: 'Free', paid: 'Paid', unknown: 'Price unknown' };

/** Assist chip showing price. Free uses the harmonised "success" custom colour. */
export function FeeChip({ fee, size }: { fee: Fee; size?: 'small' }) {
  const colours = {
    free: { bg: m3('successContainer'), fg: m3('onSuccessContainer') },
    paid: { bg: m3('secondaryContainer'), fg: m3('onSecondaryContainer') },
    unknown: { bg: m3('surfaceContainerHighest'), fg: m3('onSurfaceVariant') },
  }[fee];
  return (
    <Chip
      label={FEE_LABEL[fee]}
      size={size === 'small' ? 'small' : undefined}
      sx={{
        backgroundColor: colours.bg,
        color: colours.fg,
        ...(size === 'small' && { height: 24, '& .MuiChip-label': { px: 1 } }),
      }}
    />
  );
}

/** Facts about a bay as outlined assist chips. */
export function BayFacts({ bay }: { bay: Bay }) {
  return (
    <Stack direction="row" useFlexGap flexWrap="wrap" spacing={1}>
      <FeeChip fee={bay.fee} />
      {bay.capacity !== null && (
        <Chip variant="outlined" icon={<TwoWheelerIcon />} label={`${bay.capacity} spaces`} />
      )}
      {bay.covered !== null && (
        <Chip variant="outlined" icon={<RoofingIcon />} label={bay.covered ? 'Covered' : 'Not covered'} />
      )}
      {bay.maxStay && <Chip variant="outlined" icon={<HourglassBottomIcon />} label={`Max stay ${bay.maxStay}`} />}
      {bay.openingHours && <Chip variant="outlined" icon={<AccessTimeIcon />} label={bay.openingHours} />}
    </Stack>
  );
}
