package uk.motobay.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.motobay.app.data.ClaimResult
import uk.motobay.app.data.DisplayNames
import uk.motobay.app.data.Legal
import uk.motobay.app.data.Moderators
import uk.motobay.app.data.RecentSignInRequiredException
import uk.motobay.app.data.Settings
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.ui.LocalApp
import kotlin.math.roundToInt
import uk.motobay.app.ui.LocalRequireSignIn
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.account.SignInButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenSubmissions: () -> Unit = {}, onOpenLegal: () -> Unit = {}, onOpenPolicy: (Policy) -> Unit = {}) {
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
                            icon = {},
                            label = { IconLabel(if (settings.themeMode == mode) Icons.Filled.Check else icon, label) },
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
            DisplayNameField(settings.displayName)

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
                DeleteAccountRow()
                ListItem(
                    headlineContent = { Text("My submissions") },
                    supportingContent = { Text("Your photos, comments and bay changes, and whether they've been approved") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenSubmissions),
                )
            }

            if (settings.blocked.isNotEmpty()) {
                SectionHeader("Blocked riders")
                settings.blocked.forEach { b ->
                    ListItem(
                        headlineContent = { Text(b.name) },
                        supportingContent = { Text("Their photos and comments are hidden on this phone") },
                        trailingContent = { TextButton(onClick = { app.user.unblock(b.uid) }) { Text("Unblock") } },
                    )
                }
            }

            SectionHeader("About")
            ListItem(
                headlineContent = { Text("Legal & about") },
                supportingContent = { Text("Guidance-only notice, data licences (© OpenStreetMap contributors and others), app version") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenLegal),
            )
            ListItem(
                headlineContent = { Text("Privacy policy") },
                supportingContent = { Text("What the app collects, why, and your rights") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { onOpenPolicy(Policy.Privacy) },
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

/**
 * The name shown on the rider's submissions. Availability is checked as they
 * type; nothing changes until Save, after which the field locks until Edit.
 */
@Composable
private fun DisplayNameField(saved: String) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val requireSignIn = LocalRequireSignIn.current
    val scope = rememberCoroutineScope()
    val account by app.auth.user.collectAsStateWithLifecycle()
    var editing by rememberSaveable(saved) { mutableStateOf(saved.isBlank()) }
    var draft by rememberSaveable(saved) { mutableStateOf(saved) }
    var check by remember { mutableStateOf<NameCheck>(NameCheck.Idle) }
    var busy by remember { mutableStateOf(false) }
    val trimmed = draft.trim()
    val changed = trimmed != saved

    // Signed in on a new phone: pick up the name claimed before.
    LaunchedEffect(account?.uid) {
        if (account != null && saved.isBlank()) {
            app.displayNames.current()?.let { name -> app.user.updateSettings { it.copy(displayName = name) } }
        }
    }

    // Live check: wait for a pause in typing, then ask whether the name is free.
    LaunchedEffect(trimmed, editing, account?.uid) {
        check = when {
            !editing || !changed || trimmed.isEmpty() -> NameCheck.Idle
            DisplayNames.problem(trimmed) != null -> NameCheck.Invalid(DisplayNames.problem(trimmed)!!)
            account == null -> NameCheck.SignIn
            else -> {
                check = NameCheck.Checking
                delay(400)
                when (app.displayNames.isAvailable(trimmed)) {
                    true -> NameCheck.Available
                    false -> NameCheck.Taken
                    null -> NameCheck.Unknown
                }
            }
        }
    }

    fun save() {
        val name = trimmed
        DisplayNames.problem(name)?.let { check = NameCheck.Invalid(it); return }
        requireSignIn {
            busy = true
            scope.launch {
                val result = app.displayNames.claim(name)
                busy = false
                when (result) {
                    ClaimResult.Ok -> {
                        app.user.updateSettings { it.copy(displayName = name) }
                        editing = false
                        snackbar.showSnackbar("Display name saved")
                    }
                    ClaimResult.Taken -> check = NameCheck.Taken
                    is ClaimResult.Invalid -> check = NameCheck.Invalid(result.why)
                    ClaimResult.Failed -> snackbar.showSnackbar("Couldn't save your name. Check you're online.")
                }
            }
        }
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(DisplayNames.MAX) },
            label = { Text("Display name") },
            readOnly = !editing,
            supportingText = {
                Text(
                    when (val c = check) {
                        NameCheck.Idle -> if (saved.isBlank()) "Shown next to your photos and comments. Until you set one, you're \"${DisplayNames.ANONYMOUS}\"." else "Shown next to your photos and comments"
                        NameCheck.Checking -> "Checking…"
                        NameCheck.Available -> "✓ Available"
                        NameCheck.Taken -> "Someone already has that name"
                        NameCheck.SignIn -> "Sign in to check and save a name"
                        NameCheck.Unknown -> "Couldn't check right now. You can still try to save."
                        is NameCheck.Invalid -> c.why
                    },
                    color = when (check) {
                        NameCheck.Available -> MaterialTheme.colorScheme.primary
                        NameCheck.Taken, is NameCheck.Invalid -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            isError = check == NameCheck.Taken || check is NameCheck.Invalid,
            singleLine = true,
            enabled = !busy,
            trailingIcon = {
                if (!editing) {
                    TextButton(onClick = { editing = true }) { Text("Edit") }
                } else {
                    Row {
                        if (saved.isNotBlank()) TextButton(onClick = { draft = saved; editing = false }) { Text("Cancel") }
                        TextButton(onClick = ::save, enabled = !busy && changed && check !in setOf(NameCheck.Taken, NameCheck.Checking) && check !is NameCheck.Invalid) { Text("Save") }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("display-name"),
        )
    }
}

private sealed interface NameCheck {
    data object Idle : NameCheck
    data object Checking : NameCheck
    data object Available : NameCheck
    data object Taken : NameCheck
    data object SignIn : NameCheck
    data object Unknown : NameCheck
    data class Invalid(val why: String) : NameCheck
}

/**
 * Google Play's account-deletion rule: the rider can remove their account and
 * everything tied to it from inside the app. Firebase insists on a recent
 * sign-in first, so the dialog explains that when it happens.
 */
@Composable
private fun DeleteAccountRow() {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Delete account & data", color = MaterialTheme.colorScheme.error) },
        supportingContent = { Text("Removes your account, display name, photos, comments and bay changes. This can't be undone.") },
        modifier = Modifier.clickable(enabled = !busy) { confirm = true },
    )

    if (confirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = false },
            title = { Text("Delete your account?") },
            text = { Text("Your sign-in, display name and every photo, comment and bay change you've submitted will be deleted for good. Saved bays and settings on this phone are kept.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        val outcome = runCatching {
                            app.community.deleteEverythingByMe()
                            app.displayNames.release()
                            app.auth.deleteAccount()
                            app.user.updateSettings { it.copy(displayName = "") }
                        }
                        busy = false
                        confirm = false
                        snackbar.showSnackbar(
                            when {
                                outcome.isSuccess -> "Account deleted"
                                outcome.exceptionOrNull() is RecentSignInRequiredException ->
                                    "For safety, sign out, sign in again, then delete your account."
                                else -> "Couldn't delete your account. Check you're online and try again."
                            },
                        )
                    }
                }) { Text(if (busy) "Deleting…" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

/**
 * An icon beside a word, with the icon centred on the capital letters rather than
 * on the text's line box: that is what the eye lines up, and the line box sits a
 * little high because of the ascender space above the caps.
 */
@Composable
private fun IconLabel(icon: ImageVector, label: String) {
    val capHeightPx = with(LocalDensity.current) { (MaterialTheme.typography.labelLarge.fontSize * CAP_HEIGHT_EM).toPx() }
    Row {
        Icon(
            icon, contentDescription = null,
            Modifier.size(SegmentedButtonDefaults.IconSize)
                // The icon's "baseline" is its centre plus half a cap height, so it sits on the text baseline centred on the caps.
                .alignBy { it.measuredHeight / 2 + (capHeightPx / 2).roundToInt() },
        )
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.alignByBaseline(), maxLines = 1, softWrap = false)
    }
}

/** Roboto's capital letters are this tall relative to the font size. */
private const val CAP_HEIGHT_EM = 0.71f
