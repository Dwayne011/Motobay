package uk.motobay.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraSectionHeader
import uk.motobay.app.ui.components.FrostDivider
import uk.motobay.app.ui.components.FrostListRow
import uk.motobay.app.ui.components.FrostPageHeader
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.theme.auroraPalette
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import uk.motobay.app.BuildConfig
import uk.motobay.app.data.Legal

/** Disclaimer, data licences and the privacy policy, reachable from Settings. */
@Composable
fun LegalScreen(onBack: () -> Unit, onOpenPolicy: (Policy) -> Unit = {}) {
    val context = LocalContext.current
    val palette = auroraPalette()
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FrostPageHeader("Legal & about", palette, onBack)
        // The disclaimer in a warm-tinted panel, so it reads as a caution.
        AuroraCard(palette, color = palette.tonePaid.first.copy(alpha = 0.22f).compositeOver(palette.cardSurface), border = palette.paid.copy(alpha = 0.35f)) {
            Text("Guidance only", style = auroraStyle(15.sp, FontWeight.ExtraBold, palette.cardTitleText))
            VSpace(6)
            Text(Legal.DISCLAIMER, style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2))
            VSpace(6)
            Text(Legal.UGC_TERMS, style = auroraStyle(13.sp, FontWeight.SemiBold, palette.text2))
        }

        AuroraCard(palette, contentPadding = 4) {
            FrostListRow(
                "Privacy policy", "How MotoBays handles your account, photos and location (UK GDPR)", palette,
                trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = { onOpenPolicy(Policy.Privacy) },
            )
            FrostDivider(palette)
            FrostListRow(
                "Delete your account", "You can also delete it from Settings > Account. This page explains what is removed.", palette,
                trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = { onOpenPolicy(Policy.DeleteAccount) },
            )
        }

        AuroraSectionHeader("Data sources and licences", palette, Modifier.padding(start = 10.dp, top = 4.dp))
        AuroraCard(palette, contentPadding = 4) {
            Legal.ATTRIBUTIONS.forEachIndexed { i, a ->
                if (i > 0) FrostDivider(palette)
                FrostListRow(a.title, a.text, palette, trailing = Icons.AutoMirrored.Filled.OpenInNew, trailingDescription = "Open ${a.title} licence", onClick = { open(a.url) })
            }
        }

        AuroraCard(palette, contentPadding = 4) {
            FrostListRow("MotoBays ${BuildConfig.VERSION_NAME}", "Build ${BuildConfig.VERSION_CODE} \u00B7 ${BuildConfig.APPLICATION_ID}", palette)
        }
    }
}

/** First-launch notice. Can only be dismissed with the button, and is then remembered. */
@Composable
fun DisclaimerDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Before you ride") },
        text = { Text("${Legal.DISCLAIMER}\n\n${Legal.UGC_TERMS}") },
        confirmButton = { Button(onClick = onAccept) { Text("I understand") } },
    )
}
