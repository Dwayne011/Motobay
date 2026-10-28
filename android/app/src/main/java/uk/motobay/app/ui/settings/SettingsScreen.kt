package uk.motobay.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraHeader
import uk.motobay.app.ui.components.AuroraOutlinedButton
import uk.motobay.app.ui.components.AuroraPillTabs
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraRowText
import uk.motobay.app.ui.components.AuroraSectionHeader
import uk.motobay.app.ui.components.AuroraTag
import uk.motobay.app.ui.components.AuroraTextAction
import uk.motobay.app.ui.components.Coral
import uk.motobay.app.ui.components.Slate950
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.auroraPalette
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import uk.motobay.app.ui.LocalRequireSignIn
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.account.SignInButtons

@Composable
fun SettingsScreen(onOpenSubmissions: () -> Unit = {}, onOpenLegal: () -> Unit = {}, onOpenPolicy: (Policy) -> Unit = {}) {
    val app = LocalApp.current
    val palette = auroraPalette()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val update = app.user::updateSettings

    Column(
        Modifier.fillMaxSize().background(palette.mapBackground).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AuroraHeader("Settings", palette)

        AuroraSectionHeader("Appearance", palette)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val modes = listOf(ThemeMode.Light to "Light", ThemeMode.System to "System", ThemeMode.Dark to "Dark")
            AuroraPillTabs(
                labels = modes.map { it.second },
                selected = modes.indexOfFirst { it.first == settings.themeMode },
                palette = palette,
                onSelect = { i -> update { it.copy(themeMode = modes[i].first) } },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AuroraRowText("Use wallpaper colours", "Match the colours of your phone's wallpaper", palette)
                    Switch(
                        checked = settings.wallpaperColors,
                        onCheckedChange = { on -> update { it.copy(wallpaperColors = on) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = palette.searchIconActive, checkedThumbColor = Slate950),
                    )
                }
            }
            if (!settings.wallpaperColors || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Colour", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardTitleText))
                    Text("Every colour in the app is generated from this one.", style = auroraStyle(10.sp, color = palette.cardSubtext))
                    SeedSwatches(settings.sourceColor) { c -> update { it.copy(sourceColor = c) } }
                }
            }
        }

        AuroraSectionHeader("Profile", palette)
        DisplayNameField(settings.displayName, palette)

        AuroraSectionHeader("Account", palette)
        val account by app.auth.user.collectAsStateWithLifecycle()
        val signedIn = account
        if (signedIn == null) {
            AuroraCard(palette, radius = 16) {
                Text("Sign in to add photos, comments and bays. Browsing needs no account.", style = auroraStyle(12.sp, color = palette.cardSubtext))
                VSpace(12)
                SignInButtons()
            }
        } else {
            AuroraCard(palette, radius = 16) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(signedIn.email ?: "Signed in", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (signedIn.isModerator) {
                            AuroraTag("Moderator", palette)
                            Text("You can review submissions", style = auroraStyle(10.sp, color = palette.cardSubtext))
                        } else {
                            Text("Your submissions are reviewed before they're published", style = auroraStyle(10.sp, color = palette.cardSubtext))
                        }
                    }
                    AuroraTextAction("Sign out", Coral, onClick = app.auth::signOut)
                }
            }
            DeleteAccountRow(palette)
            AuroraCard(palette, radius = 16, onClick = onOpenSubmissions) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuroraRowText("My submissions", "Your photos, comments and bay changes, and whether they've been approved", palette)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(16.dp), tint = palette.cardSubtext)
                }
            }
        }

        if (settings.blocked.isNotEmpty()) {
            AuroraSectionHeader("Blocked riders", palette)
            AuroraCard(palette, radius = 16) {
                settings.blocked.forEachIndexed { i, b ->
                    if (i > 0) VSpace(12)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuroraRowText(b.name, "Their photos and comments are hidden on this phone", palette)
                        AuroraTextAction("Unblock", palette.searchIconActive, onClick = { app.user.unblock(b.uid) })
                    }
                }
            }
        }

        AuroraSectionHeader("About", palette)
        AuroraCard(palette, radius = 16, onClick = onOpenLegal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuroraRowText("Legal & about", "Guidance-only notice, data licences (\u00a9 OpenStreetMap contributors and others), app version", palette)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(16.dp), tint = palette.cardSubtext)
            }
        }
        AuroraCard(palette, radius = 16, onClick = { onOpenPolicy(Policy.Privacy) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuroraRowText("Privacy policy", "What the app collects, why, and your rights", palette)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(16.dp), tint = palette.cardSubtext)
            }
        }
        CrashReportBanner(palette)
        VSpace(8)
    }
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
private fun CrashReportBanner(palette: AuroraPalette) {
    val app = LocalApp.current
    val context = LocalContext.current
    var report by remember { mutableStateOf(app.crashLog.read()) }
    val text = report ?: return
    val summary = text.lineSequence().drop(1).firstOrNull { it.isNotBlank() } ?: "Report saved"
    AuroraCard(palette, radius = 16, color = Color(0x40991B1B), border = Color(0x66EF4444), contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
            Text("The app closed unexpectedly last time", style = auroraStyle(11.sp, FontWeight.Bold, Color(0xFFFEE2E2)), modifier = Modifier.weight(1f))
            AuroraTextAction("Send", palette.searchIconActive, onClick = {
                val email = app.crashLog.emailIntent(text)
                if (email.resolveActivity(context.packageManager) != null) context.startActivity(email)
                else context.startActivity(app.crashLog.shareIntent(text))
            })
            AuroraTextAction("Dismiss", palette.cardSubtext, onClick = { app.crashLog.clear(); report = null })
        }
        VSpace(6)
        Text("Sends the report to ${Moderators.SUPPORT_EMAIL}", style = auroraStyle(10.sp, color = palette.cardSubtext))
        VSpace(4)
        Text(summary, style = auroraStyle(10.sp, color = Color(0xFFFCA5A5), mono = true))
    }
}

/**
 * The name shown on the rider's submissions. Availability is checked as they
 * type; nothing changes until Save, after which the field locks until Edit.
 */
@Composable
private fun DisplayNameField(saved: String, palette: AuroraPalette) {
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

    val hint = when (val c = check) {
        NameCheck.Idle -> if (saved.isBlank()) "Shown next to your photos and comments. Until you set one, you're \"${DisplayNames.ANONYMOUS}\"." else "Shown next to your photos and comments"
        NameCheck.Checking -> "Checking\u2026"
        NameCheck.Available -> "\u2713 Available"
        NameCheck.Taken -> "Someone already has that name"
        NameCheck.SignIn -> "Sign in to check and save a name"
        NameCheck.Unknown -> "Couldn't check right now. You can still try to save."
        is NameCheck.Invalid -> c.why
    }
    val hintColor = when (check) {
        NameCheck.Available -> palette.searchIconActive
        NameCheck.Taken, is NameCheck.Invalid -> Coral
        else -> palette.cardSubtext
    }
    AuroraCard(palette, radius = 16) {
        if (!editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Display name", style = auroraStyle(10.sp, color = palette.cardSubtext))
                    Text(saved, style = auroraStyle(14.sp, FontWeight.Bold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("display-name"))
                    Text(hint, style = auroraStyle(10.sp, color = hintColor))
                }
                Surface(onClick = { editing = true }, shape = RoundedCornerShape(8.dp), color = palette.searchSurface, border = BorderStroke(1.dp, palette.cardBorder)) {
                    Text("Edit", style = auroraStyle(12.sp, FontWeight.SemiBold, palette.cardTitleText), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(DisplayNames.MAX) },
                label = { Text("Display name") },
                supportingText = { Text(hint, color = hintColor) },
                isError = check == NameCheck.Taken || check is NameCheck.Invalid,
                singleLine = true,
                enabled = !busy,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.cardTitleText, unfocusedTextColor = palette.cardTitleText,
                    focusedBorderColor = palette.searchIconActive, unfocusedBorderColor = palette.cardBorder,
                    focusedLabelColor = palette.searchIconActive, unfocusedLabelColor = palette.cardSubtext,
                    cursorColor = palette.searchIconActive,
                ),
                modifier = Modifier.fillMaxWidth().testTag("display-name"),
            )
            VSpace(8)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (saved.isNotBlank()) AuroraOutlinedButton("Cancel", palette, onClick = { draft = saved; editing = false })
                AuroraPrimaryButton(
                    "Save", palette, onClick = ::save,
                    enabled = !busy && changed && check !in setOf(NameCheck.Taken, NameCheck.Checking) && check !is NameCheck.Invalid,
                )
            }
        }
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
private fun DeleteAccountRow(palette: AuroraPalette) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    AuroraCard(palette, radius = 16, color = Color(0x1AEF4444), border = Color(0x40EF4444), onClick = { if (!busy) confirm = true }) {
        Text("Delete account & data", style = auroraStyle(13.sp, FontWeight.Bold, Coral))
        VSpace(2)
        Text("Removes your account, display name, photos, comments and bay changes. This can't be undone.", style = auroraStyle(10.sp, color = palette.cardSubtext))
    }

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

