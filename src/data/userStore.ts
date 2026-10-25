import { readStorage, writeStorage } from '../utils/storage';
import { createStore } from './createStore';
import type { Bay } from './types';

const FAVOURITES_KEY = 'motobay.favourites';
const PROFILE_KEY = 'motobay.profile';

/** Saved bays, stored as snapshots so the Saved list works offline. */
export const favouritesStore = createStore<Record<string, Bay>>(readStorage(FAVOURITES_KEY, {}));
favouritesStore.subscribe(() => writeStorage(FAVOURITES_KEY, favouritesStore.get()));

export function toggleFavourite(bay: Bay) {
  favouritesStore.set((f) => {
    const next = { ...f };
    if (next[bay.id]) delete next[bay.id];
    else next[bay.id] = bay;
    return next;
  });
}

export interface Profile {
  displayName: string;
  /**
   * Shows the Review queue. This is a local demo switch only. In production,
   * moderator access must come from the backend's auth (for example a role
   * on your account), never from the client.
   */
  moderator: boolean;
}

export const profileStore = createStore<Profile>(readStorage(PROFILE_KEY, { displayName: '', moderator: false }));
profileStore.subscribe(() => writeStorage(PROFILE_KEY, profileStore.get()));
