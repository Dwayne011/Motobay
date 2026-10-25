import { useMemo, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import ImageList from '@mui/material/ImageList';
import ImageListItem from '@mui/material/ImageListItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import AddAPhotoOutlinedIcon from '@mui/icons-material/AddAPhotoOutlined';
import { notify } from '../../components/Notifier';
import { communityStore, prepareImage, submitPhoto } from '../../data/community';
import { profileStore } from '../../data/userStore';
import type { Bay } from '../../data/types';
import { m3, m3Shape } from '../../theme/createM3Theme';

export function BayPhotos({ bay }: { bay: Bay }) {
  const allPhotos = communityStore.use((s) => s.photos);
  const displayName = profileStore.use((p) => p.displayName);
  const approved = useMemo(() => allPhotos.filter((p) => p.bayId === bay.id && p.status === 'approved'), [allPhotos, bay.id]);
  const pending = useMemo(() => allPhotos.filter((p) => p.bayId === bay.id && p.status === 'pending').length, [allPhotos, bay.id]);

  const fileInput = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState<string | null>(null);
  const [caption, setCaption] = useState('');
  const [busy, setBusy] = useState(false);
  const [viewing, setViewing] = useState<string | null>(null);

  const pick = async (file: File | undefined) => {
    if (!file) return;
    try {
      setDraft(await prepareImage(file));
    } catch {
      notify("That file couldn't be opened as a photo");
    }
  };

  const send = async () => {
    if (!draft) return;
    setBusy(true);
    await submitPhoto({ bayId: bay.id, bayName: bay.name, src: draft, caption: caption.trim(), author: displayName || 'Anonymous rider' });
    setBusy(false);
    setDraft(null);
    setCaption('');
    notify("Thanks! Your photo will appear once it's been reviewed.");
  };

  return (
    <Stack spacing={2} component="section" aria-labelledby="photos-heading">
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Typography id="photos-heading" variant="titleMedium" component="h3">
          Photos
        </Typography>
        <Button variant="outlined" startIcon={<AddAPhotoOutlinedIcon />} onClick={() => fileInput.current?.click()}>
          Add photo
        </Button>
        <input
          ref={fileInput}
          type="file"
          accept="image/*"
          capture="environment"
          hidden
          onChange={(e) => {
            void pick(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
      </Stack>

      {approved.length > 0 ? (
        <ImageList cols={3} gap={8} sx={{ m: 0 }}>
          {approved.map((p) => (
            <ImageListItem
              key={p.id}
              component="button"
              onClick={() => setViewing(p.src)}
              sx={{ border: 0, p: 0, cursor: 'pointer', borderRadius: `${m3Shape.small}px`, overflow: 'hidden' }}
            >
              <img src={p.src} alt={p.caption || `Photo of ${bay.name} by ${p.author}`} loading="lazy" style={{ aspectRatio: '1', objectFit: 'cover' }} />
            </ImageListItem>
          ))}
        </ImageList>
      ) : (
        <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant') }}>
          No photos yet. Help other riders by adding one.
        </Typography>
      )}
      {pending > 0 && (
        <Typography variant="bodySmall" sx={{ color: m3('onSurfaceVariant') }}>
          {pending} {pending === 1 ? 'photo is' : 'photos are'} waiting for review.
        </Typography>
      )}

      <Dialog open={draft !== null} onClose={() => !busy && setDraft(null)} fullWidth>
        <DialogTitle>Add a photo</DialogTitle>
        <DialogContent>
          <Stack spacing={2}>
            {draft && (
              <Box component="img" src={draft} alt="Preview" sx={{ width: '100%', borderRadius: `${m3Shape.medium}px`, maxHeight: 320, objectFit: 'cover' }} />
            )}
            <TextField
              label="Caption (optional)"
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              slotProps={{ htmlInput: { maxLength: 140 } }}
              fullWidth
            />
            <Typography variant="bodySmall">
              Photos are checked before they're published. Please avoid faces and number plates.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button variant="text" onClick={() => setDraft(null)} disabled={busy}>
            Cancel
          </Button>
          <Button variant="text" onClick={send} disabled={busy}>
            Submit
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={viewing !== null} onClose={() => setViewing(null)} maxWidth="md">
        {viewing && <Box component="img" src={viewing} alt="" sx={{ display: 'block', maxWidth: '100%', maxHeight: '80vh' }} />}
        <DialogActions>
          <Button variant="text" onClick={() => setViewing(null)}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
