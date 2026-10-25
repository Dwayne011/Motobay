import { DATA_SOURCE } from '../config';
import { createStore } from './createStore';
import { fetchBayById, fetchBaysInBBox } from './osm';
import { sampleBays, sampleBaysInBBox } from './sampleBays';
import type { BBox, Bay } from './types';

/** Bays are cached in 0.05° tiles (roughly 5 km) so panning back doesn't refetch. */
const TILE = 0.05;

interface BayState {
  bays: Record<string, Bay>;
  loading: boolean;
  error: string | null;
}

export const bayStore = createStore<BayState>({ bays: {}, loading: false, error: null });

const loadedTiles = new Set<string>();
const inflightTiles = new Set<string>();

function tilesFor(b: BBox): string[] {
  const keys: string[] = [];
  for (let y = Math.floor(b.south / TILE); y <= Math.floor(b.north / TILE); y++) {
    for (let x = Math.floor(b.west / TILE); x <= Math.floor(b.east / TILE); x++) keys.push(`${y}:${x}`);
  }
  return keys;
}

function bboxOfTiles(keys: string[]): BBox {
  const ys = keys.map((k) => Number(k.split(':')[0]));
  const xs = keys.map((k) => Number(k.split(':')[1]));
  return {
    south: Math.min(...ys) * TILE,
    north: (Math.max(...ys) + 1) * TILE,
    west: Math.min(...xs) * TILE,
    east: (Math.max(...xs) + 1) * TILE,
  };
}

function addBays(list: Bay[]) {
  bayStore.set((s) => {
    const bays = { ...s.bays };
    for (const b of list) bays[b.id] = b;
    return { ...s, bays };
  });
}

/** Loads every bay in the box, fetching only tiles not already loaded. */
export async function ensureBaysIn(b: BBox): Promise<void> {
  const missing = tilesFor(b).filter((k) => !loadedTiles.has(k) && !inflightTiles.has(k));
  if (missing.length === 0) return;
  missing.forEach((k) => inflightTiles.add(k));
  bayStore.set((s) => ({ ...s, loading: true, error: null }));
  try {
    const box = bboxOfTiles(missing);
    const list = DATA_SOURCE === 'sample' ? sampleBaysInBBox(box) : await fetchBaysInBBox(box);
    addBays(list);
    missing.forEach((k) => loadedTiles.add(k));
    bayStore.set((s) => ({ ...s, loading: inflightTiles.size > missing.length }));
  } catch (e) {
    bayStore.set((s) => ({
      ...s,
      loading: false,
      error: e instanceof Error ? e.message : 'Could not load parking bays',
    }));
  } finally {
    missing.forEach((k) => inflightTiles.delete(k));
  }
}

/** Gets one bay from cache, or fetches it (used when opening a shared link). */
export async function loadBay(id: string): Promise<Bay | null> {
  const cached = bayStore.get().bays[id];
  if (cached) return cached;
  const bay = DATA_SOURCE === 'sample' ? (sampleBays.find((b) => b.id === id) ?? null) : await fetchBayById(id);
  if (bay) addBays([bay]);
  return bay;
}
