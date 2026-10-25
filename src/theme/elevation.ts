/**
 * M3 tonal elevation. Depth is shown by blending the surface colour with a
 * primary (surface tint) overlay, not by drop shadows.
 * https://m3.material.io/styles/elevation/applying-elevation
 *
 * Levels 0–5 use the M3 tint opacities: 0%, 5%, 8%, 11%, 12%, 14%.
 * Because these are built from CSS variables, they follow light/dark
 * automatically.
 */
export type ElevationLevel = 0 | 1 | 2 | 3 | 4 | 5;

const TINT_OPACITY: Record<ElevationLevel, number> = { 0: 0, 1: 5, 2: 8, 3: 11, 4: 12, 5: 14 };

export function tonalSurface(level: ElevationLevel): string {
  const pct = TINT_OPACITY[level];
  if (pct === 0) return 'var(--mui-palette-m3-surface)';
  return `color-mix(in srgb, var(--mui-palette-m3-surfaceTint) ${pct}%, var(--mui-palette-m3-surface))`;
}

/** Maps MUI's 0–24 elevation prop onto the five M3 levels. */
export function muiElevationToM3(elevation: number): ElevationLevel {
  if (elevation <= 0) return 0;
  if (elevation === 1) return 1;
  if (elevation <= 3) return 2;
  if (elevation <= 6) return 3;
  if (elevation <= 8) return 4;
  return 5;
}

/**
 * M3 state layers: an overlay of the content ("on") colour at a fixed
 * opacity. Hover 8%, focus 10%, pressed 10%, dragged 16%.
 */
export const STATE = { hover: 8, focus: 10, pressed: 10, dragged: 16 } as const;

export function stateLayer(base: string, on: string, pct: number): string {
  return `color-mix(in srgb, ${on} ${pct}%, ${base})`;
}

/**
 * M3 keeps a soft key shadow for elements that float over arbitrary content,
 * such as a FAB over the map, where tint alone can't separate them. Standard
 * surfaces never use it.
 */
export const floatingShadow =
  '0px 1px 3px 0px rgba(0,0,0,0.3), 0px 4px 8px 3px rgba(0,0,0,0.15)';
