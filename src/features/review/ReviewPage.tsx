import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import CardMedia from '@mui/material/CardMedia';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import TaskAltIcon from '@mui/icons-material/TaskAlt';
import { notify } from '../../components/Notifier';
import { PageScaffold } from '../../components/PageScaffold';
import { SegmentedButton } from '../../components/m3/SegmentedButton';
import { communityStore, moderate } from '../../data/community';
import type { BayComment, BayPhoto } from '../../data/types';
import { m3 } from '../../theme/createM3Theme';

const when = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' });

type Kind = 'photo' | 'comment';

function decide(kind: Kind, id: string, status: 'approved' | 'rejected') {
  void moderate(kind, id, status);
  notify(`${kind === 'photo' ? 'Photo' : 'Comment'} ${status}`, {
    label: 'Undo',
    onClick: () => void moderate(kind, id, 'pending'),
  });
}

function ReviewCard({ kind, item }: { kind: Kind; item: BayPhoto | BayComment }) {
  return (
    <Card variant="outlined">
      {kind === 'photo' && (
        <CardMedia component="img" image={(item as BayPhoto).src} alt="Submitted photo" sx={{ maxHeight: 360, objectFit: 'contain', bgcolor: m3('surfaceContainerHighest') }} />
      )}
      <CardContent>
        <Typography variant="labelMedium" sx={{ color: m3('onSurfaceVariant') }}>
          {item.author} · {when.format(new Date(item.createdAt))}
        </Typography>
        <Typography variant="titleMedium" component="h2" sx={{ mt: 0.5 }}>
          <Link component={RouterLink} to={`/bay/${item.bayId}`} underline="hover" color="inherit">
            {item.bayName}
          </Link>
        </Typography>
        <Typography variant="bodyMedium" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
          {kind === 'photo' ? (item as BayPhoto).caption || <em>No caption</em> : (item as BayComment).text}
        </Typography>
      </CardContent>
      <CardActions sx={{ justifyContent: 'flex-end', gap: 1, p: 2, pt: 0 }}>
        <Button variant="outlined" onClick={() => decide(kind, item.id, 'rejected')}>
          Reject
        </Button>
        <Button variant="filled" onClick={() => decide(kind, item.id, 'approved')}>
          Approve
        </Button>
      </CardActions>
    </Card>
  );
}

/** Moderation queue: nothing riders submit is public until approved here. */
export function ReviewPage() {
  const photos = communityStore.use((s) => s.photos);
  const comments = communityStore.use((s) => s.comments);
  const pendingPhotos = useMemo(() => photos.filter((p) => p.status === 'pending'), [photos]);
  const pendingComments = useMemo(() => comments.filter((c) => c.status === 'pending'), [comments]);
  const [kind, setKind] = useState<Kind>('photo');
  const items = kind === 'photo' ? pendingPhotos : pendingComments;

  return (
    <PageScaffold title="Review">
      <Box sx={{ py: 1 }}>
        <SegmentedButton<Kind>
          aria-label="Content type"
          value={kind}
          onChange={setKind}
          fullWidth
          segments={[
            { value: 'photo', label: `Photos (${pendingPhotos.length})` },
            { value: 'comment', label: `Comments (${pendingComments.length})` },
          ]}
        />
      </Box>
      {items.length === 0 ? (
        <Stack alignItems="center" spacing={2} sx={{ py: 8 }}>
          <TaskAltIcon sx={{ fontSize: 48, color: m3('onSurfaceVariant') }} />
          <Typography variant="titleMedium">All caught up</Typography>
          <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant') }}>
            No {kind === 'photo' ? 'photos' : 'comments'} waiting for review.
          </Typography>
        </Stack>
      ) : (
        <Stack spacing={2} sx={{ mt: 2 }}>
          {items.map((item) => (
            <ReviewCard key={item.id} kind={kind} item={item} />
          ))}
        </Stack>
      )}
    </PageScaffold>
  );
}
