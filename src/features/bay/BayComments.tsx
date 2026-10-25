import { useMemo, useState } from 'react';
import Avatar from '@mui/material/Avatar';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { notify } from '../../components/Notifier';
import { communityStore, submitComment } from '../../data/community';
import { profileStore } from '../../data/userStore';
import type { Bay } from '../../data/types';
import { m3 } from '../../theme/createM3Theme';

const dateFormat = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

export function BayComments({ bay }: { bay: Bay }) {
  const all = communityStore.use((s) => s.comments);
  const displayName = profileStore.use((p) => p.displayName);
  const approved = useMemo(() => all.filter((c) => c.bayId === bay.id && c.status === 'approved'), [all, bay.id]);
  const pending = useMemo(() => all.filter((c) => c.bayId === bay.id && c.status === 'pending').length, [all, bay.id]);
  const [text, setText] = useState('');

  const send = async () => {
    if (!text.trim()) return;
    await submitComment({ bayId: bay.id, bayName: bay.name, text: text.trim(), author: displayName || 'Anonymous rider' });
    setText('');
    notify("Thanks! Your comment will appear once it's been reviewed.");
  };

  return (
    <Stack spacing={2} component="section" aria-labelledby="comments-heading">
      <Typography id="comments-heading" variant="titleMedium" component="h3">
        Comments
      </Typography>

      {approved.length === 0 && (
        <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant') }}>
          No comments yet. Is it easy to find? Is it usually full?
        </Typography>
      )}
      {approved.map((c) => (
        <Stack key={c.id} direction="row" spacing={2}>
          <Avatar sx={{ bgcolor: m3('tertiaryContainer'), color: m3('onTertiaryContainer'), width: 40, height: 40 }}>
            {c.author.charAt(0).toUpperCase()}
          </Avatar>
          <div>
            <Typography variant="labelLarge" component="p">
              {c.author}{' '}
              <Typography component="span" variant="bodySmall" sx={{ color: m3('onSurfaceVariant') }}>
                · {dateFormat.format(new Date(c.createdAt))}
              </Typography>
            </Typography>
            <Typography variant="bodyMedium" sx={{ whiteSpace: 'pre-wrap' }}>
              {c.text}
            </Typography>
          </div>
        </Stack>
      ))}
      {pending > 0 && (
        <Typography variant="bodySmall" sx={{ color: m3('onSurfaceVariant') }}>
          {pending} {pending === 1 ? 'comment is' : 'comments are'} waiting for review.
        </Typography>
      )}

      <TextField
        label="Add a comment"
        value={text}
        onChange={(e) => setText(e.target.value)}
        multiline
        minRows={2}
        slotProps={{ htmlInput: { maxLength: 500 } }}
        helperText={`Comments are checked before they're published · ${text.length}/500`}
        fullWidth
      />
      <Stack direction="row" justifyContent="flex-end">
        <Button variant="filled" onClick={send} disabled={!text.trim()}>
          Post
        </Button>
      </Stack>
    </Stack>
  );
}
