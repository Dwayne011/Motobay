import { NOMINATIM_URL } from '../config';
import type { BBox } from './types';

export interface Place {
  label: string;
  lat: number;
  lon: number;
  bbox: BBox;
}

/**
 * Looks up a UK place or postcode with OpenStreetMap Nominatim.
 * Nominatim's usage policy allows at most 1 request per second, so this is
 * only called when the user submits a search.
 */
export async function searchPlace(query: string): Promise<Place | null> {
  const url = new URL(NOMINATIM_URL);
  url.search = new URLSearchParams({ q: query, format: 'jsonv2', countrycodes: 'gb', limit: '1' }).toString();
  const res = await fetch(url, { headers: { 'Accept-Language': 'en-GB' } });
  if (!res.ok) throw new Error(`Search failed (${res.status})`);
  const results = (await res.json()) as Array<{ display_name: string; lat: string; lon: string; boundingbox: string[] }>;
  const r = results[0];
  if (!r) return null;
  const [south, north, west, east] = r.boundingbox.map(Number);
  return { label: r.display_name, lat: Number(r.lat), lon: Number(r.lon), bbox: { south, north, west, east } };
}
