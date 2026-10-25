import { OVERPASS_URL } from '../config';
import type { BBox, Bay, Fee } from './types';

interface OverpassElement {
  type: 'node' | 'way' | 'relation';
  id: number;
  lat?: number;
  lon?: number;
  center?: { lat: number; lon: number };
  tags?: Record<string, string>;
}

function parseFee(tags: Record<string, string>): Fee {
  const fee = (tags['fee'] ?? tags['fee:motorcycle'] ?? '').toLowerCase();
  if (fee === 'no') return 'free';
  if (fee === 'yes' || fee.length > 0 || tags['charge']) return 'paid';
  return 'unknown';
}

function parseCovered(value: string | undefined): boolean | null {
  if (!value) return null;
  return value === 'yes' || value === 'roof' || value === 'partial';
}

export function bayFromElement(el: OverpassElement): Bay | null {
  const lat = el.lat ?? el.center?.lat;
  const lon = el.lon ?? el.center?.lon;
  if (lat === undefined || lon === undefined) return null;
  const tags = el.tags ?? {};
  const street = tags['addr:street'] ?? null;
  const capacity = Number.parseInt(tags['capacity:motorcycle'] ?? tags['capacity'] ?? '', 10);
  return {
    id: `${el.type}/${el.id}`,
    lat,
    lon,
    name: tags['name'] ?? (street ? `${street} motorcycle bay` : 'Motorcycle parking'),
    fee: parseFee(tags),
    covered: parseCovered(tags['covered']),
    capacity: Number.isFinite(capacity) ? capacity : null,
    openingHours: tags['opening_hours'] ?? null,
    maxStay: tags['maxstay'] ?? null,
    operator: tags['operator'] ?? null,
    street,
    tags,
  };
}

async function runOverpass(query: string, signal?: AbortSignal): Promise<Bay[]> {
  const res = await fetch(OVERPASS_URL, {
    method: 'POST',
    body: new URLSearchParams({ data: query }),
    signal,
  });
  if (!res.ok) throw new Error(`Overpass error ${res.status}`);
  const json = (await res.json()) as { elements: OverpassElement[] };
  return json.elements.map(bayFromElement).filter((b): b is Bay => b !== null);
}

/**
 * Fetches motorcycle parking inside a bounding box. Includes dedicated
 * `amenity=motorcycle_parking` and general car parks tagged
 * `motorcycle=designated`.
 */
export function fetchBaysInBBox(b: BBox, signal?: AbortSignal): Promise<Bay[]> {
  const box = `${b.south},${b.west},${b.north},${b.east}`;
  const query = `[out:json][timeout:25];
(
  nwr["amenity"="motorcycle_parking"](${box});
  nwr["amenity"="parking"]["motorcycle"="designated"](${box});
);
out center tags;`;
  return runOverpass(query, signal);
}

/** Fetches a single bay by its OSM id ("node/123"), for deep links. */
export async function fetchBayById(id: string): Promise<Bay | null> {
  const [type, num] = id.split('/');
  if (!['node', 'way', 'relation'].includes(type) || !/^\d+$/.test(num)) return null;
  const bays = await runOverpass(`[out:json][timeout:25];${type}(${num});out center tags;`);
  return bays[0] ?? null;
}
