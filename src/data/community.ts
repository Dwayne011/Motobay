import { readStorage, writeStorage } from '../utils/storage';
import { createStore } from './createStore';
import type { BayComment, BayPhoto, ModerationStatus } from './types';

/**
 * Community content: photos and comments that riders submit. Everything
 * starts as "pending" and only appears publicly after a moderator approves it.
 *
 * This is a local, in-browser mock of the backend so the UI can be built
 * and tested now. Swap these functions for API calls once a backend
 * (for example Supabase or Firebase) is chosen. Components only use the
 * functions and the store below, so they won't need to change.
 */

const KEY = 'motobay.community';

interface CommunityState {
  photos: BayPhoto[];
  comments: BayComment[];
}

export const communityStore = createStore<CommunityState>(readStorage(KEY, { photos: [], comments: [] }));
communityStore.subscribe(() => {
  if (!writeStorage(KEY, communityStore.get())) {
    console.warn('Community data could not be saved locally (storage full or blocked).');
  }
});

const newId = () => crypto.randomUUID();

export async function submitPhoto(input: Pick<BayPhoto, 'bayId' | 'bayName' | 'src' | 'caption' | 'author'>) {
  const photo: BayPhoto = { ...input, id: newId(), createdAt: new Date().toISOString(), status: 'pending' };
  communityStore.set((s) => ({ ...s, photos: [photo, ...s.photos] }));
  return photo;
}

export async function submitComment(input: Pick<BayComment, 'bayId' | 'bayName' | 'text' | 'author'>) {
  const comment: BayComment = { ...input, id: newId(), createdAt: new Date().toISOString(), status: 'pending' };
  communityStore.set((s) => ({ ...s, comments: [comment, ...s.comments] }));
  return comment;
}

export async function moderate(kind: 'photo' | 'comment', id: string, status: ModerationStatus) {
  communityStore.set((s) =>
    kind === 'photo'
      ? { ...s, photos: s.photos.map((p) => (p.id === id ? { ...p, status } : p)) }
      : { ...s, comments: s.comments.map((c) => (c.id === id ? { ...c, status } : c)) },
  );
}

/** Downscales an image to at most 1280px on its longest side, as JPEG. */
export async function prepareImage(file: File, maxSize = 1280): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return canvas.toDataURL('image/jpeg', 0.8);
}
