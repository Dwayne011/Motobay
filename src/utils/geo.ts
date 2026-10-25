import type { Bay } from '../data/types';

/** Great-circle distance in metres. */
export function distanceMetres(a: { lat: number; lon: number }, b: { lat: number; lon: number }): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLon = toRad(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** UK-style distance: yards under a quarter mile, then miles. */
export function formatDistance(metres: number): string {
  const miles = metres / 1609.344;
  if (miles < 0.25) return `${Math.round(metres * 1.0936 / 10) * 10} yd`;
  return `${miles < 10 ? miles.toFixed(1) : Math.round(miles)} mi`;
}

export type MapsApp = 'google' | 'apple' | 'waze';

/** Opens turn-by-turn directions to the bay in the rider's maps app. */
export function directionsUrl(bay: Bay, app: MapsApp): string {
  const ll = `${bay.lat},${bay.lon}`;
  switch (app) {
    case 'apple':
      return `https://maps.apple.com/?daddr=${ll}&dirflg=d`;
    case 'waze':
      return `https://waze.com/ul?ll=${ll}&navigate=yes`;
    default:
      return `https://www.google.com/maps/dir/?api=1&destination=${ll}&travelmode=driving`;
  }
}

export function isAppleDevice(): boolean {
  return /iPad|iPhone|iPod|Macintosh/.test(navigator.userAgent) && 'ontouchend' in document;
}
