import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { DEFAULT_SOURCE_COLOR } from '../config';
import { readStorage, writeStorage } from '../utils/storage';
import { createM3Theme } from './createM3Theme';

interface SourceColorContextValue {
  sourceColor: string;
  setSourceColor: (hex: string) => void;
}

const SourceColorContext = createContext<SourceColorContextValue | null>(null);

const STORAGE_KEY = 'motobay.sourceColor';

/**
 * Provides the M3 theme. Changing the source colour regenerates every tonal
 * palette. Light/dark is handled by MUI's colour schemes (see useColorScheme).
 */
export function M3ThemeProvider({ children }: { children: ReactNode }) {
  const [sourceColor, setSource] = useState(() => readStorage(STORAGE_KEY, DEFAULT_SOURCE_COLOR));
  const theme = useMemo(() => createM3Theme(sourceColor), [sourceColor]);
  const value = useMemo(
    () => ({
      sourceColor,
      setSourceColor: (hex: string) => {
        setSource(hex);
        writeStorage(STORAGE_KEY, hex);
      },
    }),
    [sourceColor],
  );

  return (
    <SourceColorContext.Provider value={value}>
      <ThemeProvider theme={theme} defaultMode="system">
        <CssBaseline enableColorScheme />
        {children}
      </ThemeProvider>
    </SourceColorContext.Provider>
  );
}

export function useSourceColor() {
  const ctx = useContext(SourceColorContext);
  if (!ctx) throw new Error('useSourceColor must be used inside M3ThemeProvider');
  return ctx;
}
