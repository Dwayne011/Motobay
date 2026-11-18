package uk.motobay.app.ui.settings

import uk.motobay.app.BuildConfig
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.automirrored.outlined.Logout
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.components.FrostPageHeader
import uk.motobay.app.ui.components.FrostMenuRow
import uk.motobay.app.ui.components.FrostDivider
import uk.motobay.app.ui.components.frostOverline
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
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.HSpace
import androidx.compose.material.icons.filled.Favorite
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

/** The pages the Settings menu opens. */
enum class SettingsPage(val title: String) { Appearance("Appearance"), Profile("Profile"), Account("Account"), Blocked("Blocked riders") }

/**
 * The first Settings page: a menu, as in Frost apps. One glass card per group, each row a
 * tinted icon tile with its title and current value, opening its own page.
 */
@Composable
fun SettingsScreen(
    onOpenSubmissions: () -> Unit = {},
    onOpenLegal: () -> Unit = {},
    onOpenPolicy: (Policy) -> Unit = {},
    onOpenSupporter: () -> Unit = {},
    onOpenPage: (SettingsPage) -> Unit = {},
) {
    val app = LocalApp.current
    val palette = auroraPalette()
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val supporter by app.supporter.state.collectAsStateWithLifecycle()
    val account by app.auth.user.collectAsStateWithLifecycle()
    val community by app.community.data.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FrostPageHeader("Settings", palette, onBack = null, overline = "MotoBays ${BuildConfig.VERSION_NAME}")
        CrashReportBanner(palette)

        AuroraCard(palette, contentPadding = 4) {
            FrostMenuRow(
                Icons.Filled.Favorite, palette.toneViolet, "MotoBays Supporter",
                if (supporter.isSupporter) "Unlocked. Thank you for supporting the app."
                else "Unlock \"Free now\" and live charge times" + (supporter.price?.let { " \u00B7 $it once" } ?: ""),
                palette, onClick = onOpenSupporter,
            )
        }

        AuroraCard(palette, contentPadding = 4) {
            val theme = when (settings.themeMode) { ThemeMode.Light -> "Light"; ThemeMode.Dark -> "Dark"; ThemeMode.System -> "Same as your phone" }
            FrostMenuRow(Icons.Outlined.Palette, palette.toneBlue, "Appearance", theme, palette, onClick = { onOpenPage(SettingsPage.Appearance) })
            FrostDivider(palette)
            FrostMenuRow(
                Icons.Outlined.Badge, palette.toneFree, "Profile",
                if (settings.displayName.isBlank()) "No display name yet" else "Shown as ${settings.displayName} on your posts",
                palette, onClick = { onOpenPage(SettingsPage.Profile) },
            )
            FrostDivider(palette)
            val signedIn = account
            FrostMenuRow(
                Icons.Outlined.AccountCircle, palette.toneViolet, "Account",
                when {
                    signedIn == null -> "Not signed in. Sign in to post photos and comments"
                    signedIn.isModerator -> "${signedIn.email ?: "Signed in"} \u00B7 Moderator"
                    else -> signedIn.email ?: "Signed in"
                },
                palette, onClick = { onOpenPage(SettingsPage.Account) },
            )
            if (signedIn != null) {
                val mine = community.photos.filter { it.authorUid == signedIn.uid }.map { it.status } +
                    community.comments.filter { it.authorUid == signedIn.uid }.map { it.status } +
                    community.bays.filter { it.authorUid == signedIn.uid }.map { it.status }
                val waiting = mine.count { it == ModerationStatus.Pending }
                val approved = mine.count { it == ModerationStatus.Approved }
                FrostDivider(palette)
                FrostMenuRow(
                    Icons.Outlined.Inventory2, palette.tonePaid, "My submissions",
                    if (mine.isEmpty()) "Nothing yet" else "$waiting waiting \u00B7 $approved approved",
                    palette, onClick = onOpenSubmissions,
                )
            }
            if (settings.blocked.isNotEmpty()) {
                FrostDivider(palette)
                FrostMenuRow(
                    Icons.Outlined.Block, palette.tile to palette.text2, "Blocked riders",
                    "${settings.blocked.size} hidden on this phone", palette, onClick = { onOpenPage(SettingsPage.Blocked) },
                )
            }
            FrostDivider(palette)
            FrostMenuRow(
                Icons.Outlined.Gavel, palette.tile to palette.text2, "Legal & about",
                "Privacy policy, data sources, licences", palette, onClick = onOpenLegal,
            )
        }
    }
}

/** One of the pages the Settings menu opens. */
@Composable
fun SettingsPageScreen(page: SettingsPage, onBack: () -> Unit) {
    val palette = auroraPalette()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FrostPageHeader(page.title, palette, onBack, overline = "Settings")
        when (page) {
            SettingsPage.Appearance -> AppearancePage(palette)
            SettingsPage.Profile -> {
                val app = LocalApp.current
                val settings by app.user.settings.collectAsStateWithLifecycle()
                DisplayNameField(settings.displayName, palette)
            }
            SettingsPage.Account -> AccountPage(palette)
            SettingsPage.Blocked -> BlockedPage(palette)
        }
    }
}

@Composable
private fun AppearancePage(palette: AuroraPalette) {
    val app = LocalApp.current
    val settings by app.user.settings.collectAsStateWithLifecycle()
    AuroraSectionHeader("Theme", palette, Modifier.padding(start = 10.dp))
    AuroraCard(palette, contentPadding = 12) {
        val modes = listOf(ThemeMode.Light to "Light", ThemeMode.System to "System", ThemeMode.Dark to "Dark")
        AuroraPillTabs(
            labels = modes.map { it.second },
            selected = modes.indexOfFirst { it.first == settings.themeMode },
            palette = palette,
            onSelect = { i -> app.user.updateSettings { it.copy(themeMode = modes[i].first) } },
        )
        VSpace(8)
        Text(
            "System follows your phone's light or dark mode.",
            style = auroraStyle(11.sp, FontWeight.SemiBold, palette.cardSubtext),
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun AccountPage(palette: AuroraPalette) {
    val app = LocalApp.current
    val account by app.auth.user.collectAsStateWithLifecycle()
    val signedIn = account
    if (signedIn == null) {
        AuroraCard(palette) {
            Text("Sign in to add photos, comments and bays. Browsing needs no account.", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2))
            VSpace(12)
            SignInButtons()
        }
        return
    }
    AuroraCard(palette, contentPadding = 4) {
        FrostMenuRow(
            Icons.Outlined.AccountCircle, palette.toneViolet, signedIn.email ?: "Signed in",
            if (signedIn.isModerator) "Moderator: you can review submissions" else "Your submissions are reviewed before they're published",
            palette, onClick = null,
        )
        FrostDivider(palette)
        FrostMenuRow(
            Icons.AutoMirrored.Outlined.Logout, palette.tile to palette.text2, "Sign out", null, palette,
            onClick = app.auth::signOut, trailing = {},
        )
    }
    AuroraSectionHeader("Danger zone", palette, Modifier.padding(start = 10.dp, top = 4.dp), color = Coral)
    DeleteAccountRow(palette)
}

@Composable
private fun BlockedPage(palette: AuroraPalette) {
    val app = LocalApp.current
    val settings by app.user.settings.collectAsStateWithLifecycle()
    if (settings.blocked.isEmpty()) {
        Text("You haven't blocked anyone.", style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardSubtext), modifier = Modifier.padding(horizontal = 10.dp))
        return
    }
    AuroraCard(palette, contentPadding = 4) {
        settings.blocked.forEachIndexed { i, b ->
            if (i > 0) FrostDivider(palette)
            FrostMenuRow(
                Icons.Outlined.Block, palette.tile to palette.text2, b.name, "Their photos and comments are hidden on this phone", palette,
                onClick = null, trailing = { AuroraTextAction("Unblock", palette.accent, onClick = { app.user.unblock(b.uid) }) },
            )
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
    AuroraCard(palette, color = Color(0x40991B1B), border = Color(0x66EF4444), contentPadding = 14) {
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
    AuroraCard(palette) {
        if (!editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("DISPLAY NAME", style = frostOverline(palette.cardSubtext))
                    Text(saved, style = auroraStyle(17.sp, FontWeight.ExtraBold, palette.cardTitleText), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("display-name"))
                    Text(hint, style = auroraStyle(11.sp, FontWeight.SemiBold, hintColor))
                }
                AuroraOutlinedButton("Edit", palette, onClick = { editing = true })
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
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.cardTitleText, unfocusedTextColor = palette.cardTitleText,
                    focusedContainerColor = palette.tile, unfocusedContainerColor = palette.tile,
                    focusedBorderColor = palette.selectedBorder, unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = palette.accent, unfocusedLabelColor = palette.cardSubtext,
                    cursorColor = palette.accent,
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

    AuroraCard(palette, contentPadding = 4, border = Coral.copy(alpha = 0.35f)) {
        FrostMenuRow(
            Icons.Outlined.DeleteForever, Coral.copy(alpha = 0.16f) to Coral, "Delete account & data",
            "Removes your account, display name, photos, comments, ratings and bay changes. This can't be undone.",
            palette, onClick = { if (!busy) confirm = true }, titleColor = Coral, trailing = {},
        )
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = false },
            title = { Text("Delete your account?") },
            text = { Text("Your sign-in, display name and every photo, comment, rating, report and bay change you've submitted will be deleted for good. Saved bays and settings on this phone are kept.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        val outcome = runCatching {
                            app.community.deleteEverythingByMe()
                            app.bayStatus.deleteMine()
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

