package uk.motobay.app.ui.settings

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.R
import uk.motobay.app.data.Settings
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.ui.LocalApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = LocalApp.current
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val update = app.user::updateSettings
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { TopAppBar(title = { Text("Settings") }, scrollBehavior = scroll) },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SectionHeader("Appearance")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow {
                    val modes = listOf(
                        Triple(ThemeMode.Light, "Light", Icons.Outlined.LightMode),
                        Triple(ThemeMode.System, "System", Icons.Outlined.SettingsBrightness),
                        Triple(ThemeMode.Dark, "Dark", Icons.Outlined.DarkMode),
                    )
                    modes.forEachIndexed { i, (mode, label, icon) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = settings.themeMode == mode) {
                                    Icon(icon, contentDescription = null, Modifier.size(SegmentedButtonDefaults.IconSize))
                                }
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Use wallpaper colours") },
                    supportingContent = { Text("Match the colours of your phone's wallpaper") },
                    trailingContent = {
                        Switch(checked = settings.wallpaperColors, onCheckedChange = { on -> update { it.copy(wallpaperColors = on) } })
                    },
                )
            }

            if (!settings.wallpaperColors || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Colour", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Every colour in the app is generated from this one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SeedSwatches(settings.sourceColor) { c -> update { it.copy(sourceColor = c) } }
                }
            }
            PalettePreview(MaterialTheme.colorScheme, Modifier.padding(16.dp))

            SectionHeader("Profile")
            OutlinedTextField(
                value = settings.displayName,
                onValueChange = { v -> update { it.copy(displayName = v.take(40)) } },
                label = { Text("Display name") },
                supportingText = { Text("Shown next to your photos and comments") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            SectionHeader("Moderation")
            ListItem(
                headlineContent = { Text("Moderator mode") },
                supportingContent = { Text("Demo only. Shows the review queue. Real moderator access will come from your account.") },
                trailingContent = {
                    Switch(checked = settings.moderator, onCheckedChange = { on -> update { it.copy(moderator = on) } })
                },
            )

            SectionHeader("About")
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = {
                    Text("Bay locations © OpenStreetMap contributors, under the Open Database Licence. Map tiles by OpenFreeMap. Place search by Nominatim.")
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeedSwatches(selected: Long, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Settings.PRESETS.forEach { argb ->
            val isSelected = argb == selected
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable { onSelect(argb) }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = "Colour #%06X".format(argb and 0xFFFFFF)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = Color(argb), shape = CircleShape, modifier = Modifier.size(if (isSelected) 38.dp else 48.dp)) {
                    if (isSelected) Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color.White) }
                }
            }
        }
    }
}

/** Shows the generated roles and the five tonal elevation levels. */
@Composable
private fun PalettePreview(scheme: ColorScheme, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val roles = listOf(
            Triple("Primary", scheme.primary, scheme.onPrimary),
            Triple("Secondary", scheme.secondary, scheme.onSecondary),
            Triple("Tertiary", scheme.tertiary, scheme.onTertiary),
            Triple("Primary container", scheme.primaryContainer, scheme.onPrimaryContainer),
            Triple("Secondary container", scheme.secondaryContainer, scheme.onSecondaryContainer),
            Triple("Tertiary container", scheme.tertiaryContainer, scheme.onTertiaryContainer),
        )
        roles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, bg, fg) ->
                    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
        Text(
            "Tonal elevation, levels 0–5",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // M3 elevation levels 0–5 map to 0, 1, 3, 6, 8 and 12dp of tonal elevation.
            listOf(0, 1, 3, 6, 8, 12).forEachIndexed { level, dp ->
                Surface(
                    tonalElevation = dp.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(56.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("$level", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}
