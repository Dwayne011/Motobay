import { useCallback } from 'react';
import { createStore } from '../data/createStore';

interface LocationState {
  position: { lat: number; lon: number } | null;
  locating: boolean;
  error: string | null;
}

const locationStore = createStore<LocationState>({ position: null, locating: false, error: null });

/**
 * The rider's position, shared across screens. Only requested when the
 * user taps "my location", and never stored or sent anywhere.
 */
export function useUserLocation() {
  const state = locationStore.use((s) => s);

  const locate = useCallback(
    () =>
      new Promise<{ lat: number; lon: number } | null>((resolve) => {
        if (!('geolocation' in navigator)) {
          locationStore.set((s) => ({ ...s, error: 'Location is not available on this device' }));
          resolve(null);
          return;
        }
        locationStore.set((s) => ({ ...s, locating: true, error: null }));
        navigator.geolocation.getCurrentPosition(
          (p) => {
            const position = { lat: p.coords.latitude, lon: p.coords.longitude };
            locationStore.set({ position, locating: false, error: null });
            resolve(position);
          },
          (err) => {
            locationStore.set((s) => ({
              ...s,
              locating: false,
              error: err.code === err.PERMISSION_DENIED ? 'Location permission was denied' : 'Could not find your location',
            }));
            resolve(null);
          },
          { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
        );
      }),
    [],
  );

  return { ...state, locate };
}
