import Box from '@mui/material/Box';
import ButtonBase from '@mui/material/ButtonBase';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import ListSubheader from '@mui/material/ListSubheader';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useColorScheme } from '@mui/material/styles';
import CheckIcon from '@mui/icons-material/Check';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import SettingsBrightnessIcon from '@mui/icons-material/SettingsBrightness';
import { APP_NAME, SOURCE_COLOR_PRESETS } from '../../config';
import { PageScaffold } from '../../components/PageScaffold';
import { SegmentedButton } from '../../components/m3/SegmentedButton';
import { profileStore } from '../../data/userStore';
import { m3, m3Shape, type M3PaletteTokens } from '../../theme/createM3Theme';
import { useSourceColor } from '../../theme/M3ThemeProvider';
import type { ElevationLevel } from '../../theme/elevation';

type Mode = 'light' | 'system' | 'dark';

function Swatch({ hex, selected, onClick }: { hex: string; selected: boolean; onClick: () => void }) {
  return (
    <ButtonBase
      onClick={onClick}
      aria-label={`Use colour ${hex}`}
      aria-pressed={selected}
      sx={{
        width: 48,
        height: 48,
        borderRadius: m3Shape.full,
        backgroundColor: hex,
        color: '#fff',
        outline: selected ? `3px solid ${m3('onSurface')}` : 'none',
        outlineOffset: 2,
      }}
    >
      {selected && <CheckIcon />}
    </ButtonBase>
  );
}

const ROLE_PREVIEW: Array<[keyof M3PaletteTokens, keyof M3PaletteTokens, string]> = [
  ['primary', 'onPrimary', 'Primary'],
  ['secondary', 'onSecondary', 'Secondary'],
  ['tertiary', 'onTertiary', 'Tertiary'],
  ['primaryContainer', 'onPrimaryContainer', 'Primary container'],
  ['secondaryContainer', 'onSecondaryContainer', 'Secondary container'],
  ['tertiaryContainer', 'onTertiaryContainer', 'Tertiary container'],
];

function PalettePreview() {
  return (
    <Stack spacing={2}>
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1 }}>
        {ROLE_PREVIEW.map(([bg, fg, label]) => (
          <Box key={bg} sx={{ backgroundColor: m3(bg), color: m3(fg), borderRadius: `${m3Shape.medium}px`, p: 1.5, minHeight: 56 }}>
            <Typography variant="labelMedium">{label}</Typography>
          </Box>
        ))}
      </Box>
      <Typography variant="labelLarge" sx={{ color: m3('onSurfaceVariant') }}>
        Tonal elevation, levels 0–5
      </Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: 1 }}>
        {([0, 1, 2, 3, 6, 12] as const).map((elevation, level) => (
          <Paper
            key={level}
            elevation={elevation}
            variant="elevation"
            sx={{ height: 56, borderRadius: `${m3Shape.medium}px`, display: 'grid', placeItems: 'center', border: `1px solid ${m3('outlineVariant')}` }}
          >
            <Typography variant="labelMedium">{level as ElevationLevel}</Typography>
          </Paper>
        ))}
      </Box>
    </Stack>
  );
}

export function SettingsPage() {
  const { mode, setMode } = useColorScheme();
  const { sourceColor, setSourceColor } = useSourceColor();
  const profile = profileStore.use((p) => p);
  const isPreset = SOURCE_COLOR_PRESETS.includes(sourceColor);

  return (
    <PageScaffold title="Settings">
      <List>
        <ListSubheader>Appearance</ListSubheader>
        <ListItem sx={{ display: 'block' }}>
          <Typography variant="bodyLarge" sx={{ mb: 1.5 }}>
            Theme
          </Typography>
          <SegmentedButton<Mode>
            aria-label="Theme"
            value={(mode ?? 'system') as Mode}
            onChange={setMode}
            segments={[
              { value: 'light', label: 'Light', icon: <LightModeOutlinedIcon /> },
              { value: 'system', label: 'System', icon: <SettingsBrightnessIcon /> },
              { value: 'dark', label: 'Dark', icon: <DarkModeOutlinedIcon /> },
            ]}
          />
        </ListItem>
        <ListItem sx={{ display: 'block' }}>
          <Typography variant="bodyLarge">Colour</Typography>
          <Typography variant="bodyMedium" sx={{ color: m3('onSurfaceVariant'), mb: 1.5 }}>
            Every colour in the app is generated from this one.
          </Typography>
          <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap" alignItems="center">
            {SOURCE_COLOR_PRESETS.map((hex) => (
              <Swatch key={hex} hex={hex} selected={hex === sourceColor} onClick={() => setSourceColor(hex)} />
            ))}
            <Box
              component="label"
              sx={{
                width: 48,
                height: 48,
                borderRadius: m3Shape.full,
                display: 'grid',
                placeItems: 'center',
                cursor: 'pointer',
                border: `1px dashed ${m3('outline')}`,
                background: isPreset ? 'conic-gradient(red, yellow, lime, aqua, blue, magenta, red)' : sourceColor,
                outline: isPreset ? 'none' : `3px solid ${m3('onSurface')}`,
                outlineOffset: 2,
              }}
            >
              <input
                type="color"
                value={sourceColor}
                onChange={(e) => setSourceColor(e.target.value)}
                aria-label="Custom colour"
                style={{ opacity: 0, width: 0, height: 0 }}
              />
            </Box>
          </Stack>
        </ListItem>
        <ListItem sx={{ display: 'block' }}>
          <PalettePreview />
        </ListItem>

        <ListSubheader sx={{ mt: 2 }}>Profile</ListSubheader>
        <ListItem>
          <TextField
            label="Display name"
            helperText="Shown next to your photos and comments"
            value={profile.displayName}
            onChange={(e) => profileStore.set((p) => ({ ...p, displayName: e.target.value }))}
            slotProps={{ htmlInput: { maxLength: 40 } }}
            fullWidth
          />
        </ListItem>

        <ListSubheader sx={{ mt: 2 }}>Moderation</ListSubheader>
        <ListItem
          secondaryAction={
            <Switch
              edge="end"
              checked={profile.moderator}
              onChange={(e) => profileStore.set((p) => ({ ...p, moderator: e.target.checked }))}
              slotProps={{ input: { 'aria-label': 'Moderator mode' } }}
            />
          }
        >
          <ListItemText
            primary="Moderator mode"
            secondary="Demo only. Shows the review queue. Real moderator access will come from your account."
            sx={{ pr: 6 }}
          />
        </ListItem>

        <ListSubheader sx={{ mt: 2 }}>About</ListSubheader>
        <ListItem>
          <ListItemText
            primary={APP_NAME}
            secondary="Map data and bay locations © OpenStreetMap contributors, under the Open Database Licence. Place search by Nominatim."
          />
        </ListItem>
      </List>
    </PageScaffold>
  );
}
