import type { BBox, Bay } from './types';

/**
 * Made-up bays for offline development (VITE_DATA_SOURCE=sample).
 * These are NOT real locations and must never ship to users.
 */
const SAMPLES: Array<Partial<Bay> & Pick<Bay, 'lat' | 'lon' | 'fee'>> = [
  { lat: 51.5136, lon: -0.1365, fee: 'free', capacity: 12, covered: false },
  { lat: 51.5101, lon: -0.1342, fee: 'paid', capacity: 8, maxStay: '4 hours' },
  { lat: 51.5155, lon: -0.1418, fee: 'unknown', covered: true },
  { lat: 51.5079, lon: -0.1281, fee: 'free', capacity: 20, openingHours: '24/7' },
  { lat: 51.5122, lon: -0.1236, fee: 'paid', capacity: 6 },
  { lat: 53.4794, lon: -2.2453, fee: 'free', capacity: 10 },
  { lat: 55.9533, lon: -3.1883, fee: 'paid', capacity: 15 },
];

export const sampleBays: Bay[] = SAMPLES.map((s, i) => ({
  id: `sample/${i + 1}`,
  name: `Sample bay ${i + 1}`,
  covered: null,
  capacity: null,
  openingHours: null,
  maxStay: null,
  operator: null,
  street: null,
  tags: { amenity: 'motorcycle_parking', note: 'Development sample, not a real bay' },
  ...s,
}));

export function sampleBaysInBBox(b: BBox): Bay[] {
  return sampleBays.filter((x) => x.lat >= b.south && x.lat <= b.north && x.lon >= b.west && x.lon <= b.east);
}
