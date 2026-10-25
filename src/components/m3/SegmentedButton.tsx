import type { ReactNode } from 'react';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import CheckIcon from '@mui/icons-material/Check';
import { m3, m3Shape } from '../../theme/createM3Theme';
import { STATE, stateLayer } from '../../theme/elevation';
import { m3TypeScale } from '../../theme/typography';

export interface Segment<T extends string> {
  value: T;
  label: string;
  icon?: ReactNode;
}

interface Props<T extends string> {
  segments: Segment<T>[];
  value: T;
  onChange: (value: T) => void;
  'aria-label': string;
  /** Stretch to fill the container width. */
  fullWidth?: boolean;
  /** Hide labels and show icons only. */
  iconOnly?: boolean;
}

/**
 * M3 Segmented button (single-select). 40dp tall, fully rounded outer
 * corners, a 1dp outline, secondaryContainer for the selected segment, and a
 * check mark that replaces the icon when selected.
 * https://m3.material.io/components/segmented-buttons/specs
 *
 * MUI v6 has no segmented button, so this is built on ToggleButtonGroup,
 * which gives the radio-group semantics and keyboard handling.
 */
export function SegmentedButton<T extends string>({
  segments,
  value,
  onChange,
  fullWidth,
  iconOnly,
  ...rest
}: Props<T>) {
  return (
    <ToggleButtonGroup
      exclusive
      value={value}
      onChange={(_, v: T | null) => v !== null && onChange(v)}
      aria-label={rest['aria-label']}
      fullWidth={fullWidth}
      sx={{
        height: 40,
        borderRadius: m3Shape.full,
        backgroundColor: m3('surface'),
        '& .MuiToggleButtonGroup-grouped': {
          border: `1px solid ${m3('outline')}`,
          '&:not(:first-of-type)': { marginLeft: '-1px', borderLeft: `1px solid ${m3('outline')}` },
        },
        '& .MuiToggleButtonGroup-firstButton': {
          borderTopLeftRadius: m3Shape.full,
          borderBottomLeftRadius: m3Shape.full,
        },
        '& .MuiToggleButtonGroup-lastButton': {
          borderTopRightRadius: m3Shape.full,
          borderBottomRightRadius: m3Shape.full,
        },
      }}
    >
      {segments.map((s) => {
        const selected = s.value === value;
        const icon = selected ? <CheckIcon /> : s.icon;
        return (
          <ToggleButton
            key={s.value}
            value={s.value}
            aria-label={s.label}
            sx={{
              ...m3TypeScale.labelLarge,
              textTransform: 'none',
              px: iconOnly ? 2 : '12px',
              minWidth: 48,
              gap: '8px',
              borderRadius: 0,
              color: m3('onSurface'),
              '& .MuiSvgIcon-root': { fontSize: 18 },
              '&:hover': { backgroundColor: `color-mix(in srgb, ${m3('onSurface')} ${STATE.hover}%, transparent)` },
              '&.Mui-selected': {
                backgroundColor: m3('secondaryContainer'),
                color: m3('onSecondaryContainer'),
                '&:hover': {
                  backgroundColor: stateLayer(m3('secondaryContainer'), m3('onSecondaryContainer'), STATE.hover),
                },
              },
            }}
          >
            {icon}
            {!iconOnly && s.label}
          </ToggleButton>
        );
      })}
    </ToggleButtonGroup>
  );
}
