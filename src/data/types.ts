export type Fee = 'free' | 'paid' | 'unknown';

/** A motorcycle parking location, normalised from OpenStreetMap tags. */
export interface Bay {
  /** OSM element id, for example "node/123456". */
  id: string;
  lat: number;
  lon: number;
  name: string;
  fee: Fee;
  covered: boolean | null;
  capacity: number | null;
  openingHours: string | null;
  maxStay: string | null;
  operator: string | null;
  street: string | null;
  /** Every raw OSM tag, for the details section. */
  tags: Record<string, string>;
}

export interface BBox {
  south: number;
  west: number;
  north: number;
  east: number;
}

export type ModerationStatus = 'pending' | 'approved' | 'rejected';

export interface BayPhoto {
  id: string;
  bayId: string;
  bayName: string;
  /** Downscaled JPEG as a data URL (local mock). A real backend would return a URL. */
  src: string;
  caption: string;
  author: string;
  createdAt: string;
  status: ModerationStatus;
}

export interface BayComment {
  id: string;
  bayId: string;
  bayName: string;
  text: string;
  author: string;
  createdAt: string;
  status: ModerationStatus;
}
