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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.R
import uk.motobay.app.data.Moderators
import uk.motobay.app.data.Settings
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.account.SignInButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenSubmissions: () -> Unit = {}) {
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

            SectionHeader("Profile")
            OutlinedTextField(
                value = settings.displayName,
                onValueChange = { v -> update { it.copy(displayName = v.take(40)) } },
                label = { Text("Display name") },
                supportingText = { Text("Shown next to your photos and comments") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            SectionHeader("Account")
            val account by app.auth.user.collectAsStateWithLifecycle()
            val signedIn = account
            if (signedIn == null) {
                Text(
                    "Sign in to add photos, comments and bays. Browsing needs no account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SignInButtons(Modifier.padding(horizontal = 16.dp))
            } else {
                ListItem(
                    headlineContent = { Text(signedIn.email ?: "Signed in") },
                    supportingContent = { Text(if (signedIn.isModerator) "Moderator: you can review submissions" else "Your submissions are reviewed before they're published") },
                    trailingContent = { TextButton(onClick = app.auth::signOut) { Text("Sign out") } },
                )
                ListItem(
                    headlineContent = { Text("My submissions") },
                    supportingContent = { Text("Your photos, comments and bay changes, and whether they've been approved") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenSubmissions),
                )
            }

            SectionHeader("About")
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = {
                    Text("Bay locations © OpenStreetMap contributors, under the Open Database Licence. Map tiles by OpenFreeMap. Place search by Nominatim.")
                },
            )
            CrashReportRow()
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

/** Shown only after a crash: lets the tester send the stack trace on. */
@Composable
private fun CrashReportRow() {
    val app = LocalApp.current
    val context = LocalContext.current
    var report by remember { mutableStateOf(app.crashLog.read()) }
    val text = report ?: return
    ListItem(
        headlineContent = { Text("The app closed unexpectedly last time") },
        overlineContent = { Text("Send the report to ${Moderators.SUPPORT_EMAIL}") },
        supportingContent = { Text(text.lineSequence().drop(1).firstOrNull { it.isNotBlank() } ?: "Report saved") },
        leadingContent = { Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        trailingContent = {
            Row {
                TextButton(onClick = {
                    val email = app.crashLog.emailIntent(text)
                    if (email.resolveActivity(context.packageManager) != null) context.startActivity(email)
                    else context.startActivity(app.crashLog.shareIntent(text))
                }) { Text("Send") }
                TextButton(onClick = { app.crashLog.clear(); report = null }) { Text("Dismiss") }
            }
        },
    )
}
