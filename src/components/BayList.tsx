import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import TwoWheelerIcon from '@mui/icons-material/TwoWheeler';
import type { Bay } from '../data/types';
import { m3 } from '../theme/createM3Theme';
import { formatDistance } from '../utils/geo';
import { FeeChip } from './BayChips';

export interface BayWithDistance {
  bay: Bay;
  distance: number | null;
}

interface Props {
  items: BayWithDistance[];
  selectedId?: string | null;
  onSelect: (bay: Bay) => void;
}

/** M3 two-line list of bays. */
export function BayList({ items, selectedId, onSelect }: Props) {
  return (
    <List disablePadding>
      {items.map(({ bay, distance }) => (
        <ListItemButton
          key={bay.id}
          selected={bay.id === selectedId}
          onClick={() => onSelect(bay)}
          sx={{ py: 1.5, px: 2, gap: 2, borderRadius: 3, mx: 1 }}
        >
          <ListItemIcon sx={{ minWidth: 0 }}>
            <TwoWheelerIcon />
          </ListItemIcon>
          <ListItemText
            primary={bay.name}
            secondary={
              <Stack component="span" direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                <FeeChip fee={bay.fee} size="small" />
                {bay.capacity !== null && <span>{bay.capacity} spaces</span>}
              </Stack>
            }
            secondaryTypographyProps={{ component: 'span' }}
          />
          {distance !== null && (
            <Typography variant="labelSmall" sx={{ color: m3('onSurfaceVariant'), flexShrink: 0 }}>
              {formatDistance(distance)}
            </Typography>
          )}
        </ListItemButton>
      ))}
    </List>
  );
}
