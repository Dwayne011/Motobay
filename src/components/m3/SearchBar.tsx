import { useState, type FormEvent, type ReactNode } from 'react';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import InputBase from '@mui/material/InputBase';
import CloseIcon from '@mui/icons-material/Close';
import SearchIcon from '@mui/icons-material/Search';
import { m3, m3Shape } from '../../theme/createM3Theme';
import { floatingShadow } from '../../theme/elevation';
import { m3TypeScale } from '../../theme/typography';

interface Props {
  placeholder: string;
  onSubmit: (query: string) => void;
  loading?: boolean;
  /** Trailing action, for example an avatar or a filter button. */
  trailing?: ReactNode;
  /** Adds a key shadow when the bar floats over the map. */
  floating?: boolean;
}

/**
 * M3 Search bar: 56dp tall, fully rounded, on surfaceContainerHigh.
 * https://m3.material.io/components/search/specs
 */
export function SearchBar({ placeholder, onSubmit, loading, trailing, floating }: Props) {
  const [query, setQuery] = useState('');
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (query.trim()) onSubmit(query.trim());
  };

  return (
    <Box
      component="form"
      role="search"
      onSubmit={submit}
      sx={{
        height: 56,
        display: 'flex',
        alignItems: 'center',
        gap: '4px',
        pl: '4px',
        pr: '4px',
        borderRadius: m3Shape.full,
        backgroundColor: m3('surfaceContainerHigh'),
        boxShadow: floating ? floatingShadow : 'none',
      }}
    >
      <IconButton type="submit" aria-label="Search" sx={{ color: m3('onSurface') }}>
        {loading ? <CircularProgress size={20} /> : <SearchIcon />}
      </IconButton>
      <InputBase
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
        inputProps={{ 'aria-label': placeholder, enterKeyHint: 'search' }}
        sx={{
          flex: 1,
          ...m3TypeScale.bodyLarge,
          color: m3('onSurface'),
          '& input::placeholder': { color: m3('onSurfaceVariant'), opacity: 1 },
        }}
      />
      {query && (
        <IconButton aria-label="Clear search" onClick={() => setQuery('')}>
          <CloseIcon />
        </IconButton>
      )}
      {trailing}
    </Box>
  );
}
