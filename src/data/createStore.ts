import { useSyncExternalStore } from 'react';

/** Minimal external store, read in components with useSyncExternalStore. */
export function createStore<T>(initial: T) {
  let state = initial;
  const listeners = new Set<() => void>();
  const subscribe = (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  };
  return {
    get: () => state,
    set(next: T | ((prev: T) => T)) {
      state = typeof next === 'function' ? (next as (prev: T) => T)(state) : next;
      listeners.forEach((l) => l());
    },
    subscribe,
    /**
     * Reads part of the state. The selector must return an existing
     * reference (not a new array or object); derive values with useMemo.
     */
    use<S>(selector: (s: T) => S): S {
      return useSyncExternalStore(subscribe, () => selector(state));
    },
  };
}
