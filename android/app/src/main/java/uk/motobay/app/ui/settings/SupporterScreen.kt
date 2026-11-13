package uk.motobay.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.motobay.app.BuildConfig
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.components.AuroraCard
import uk.motobay.app.ui.components.AuroraHeader
import uk.motobay.app.ui.components.AuroraPrimaryButton
import uk.motobay.app.ui.components.AuroraTextAction
import uk.motobay.app.ui.components.HSpace
import uk.motobay.app.ui.components.VSpace
import uk.motobay.app.ui.components.auroraStyle
import uk.motobay.app.ui.theme.AuroraPalette
import uk.motobay.app.ui.theme.auroraPalette

/** What Supporter is, what it costs, and the button that buys it through Google Play. */
@Composable
fun SupporterScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val palette = auroraPalette()
    val state by app.supporter.state.collectAsStateWithLifecycle()
    // Pick up the price, and a purchase made on another phone, each time the page opens.
    LaunchedEffect(Unit) { app.supporter.refresh() }

    Column(
        Modifier.fillMaxSize().background(palette.mapBackground).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.cardTitleText) }
            AuroraHeader("MotoBays Supporter", palette)
        }
        Text(
            if (state.isSupporter) "You're a Supporter. Thank you for keeping MotoBays running." else
                "MotoBays is free, with no adverts and no tracking. A one-off payment keeps it that way and unlocks two extras.",
            style = auroraStyle(13.sp, color = palette.cardTitleText),
        )

        Benefit(Icons.Outlined.Schedule, "\"Free now\" filter", "Show only the bays you can park in for free at this moment, including paid bays outside their charging hours.", palette)
        Benefit(Icons.Outlined.Timelapse, "Live charge status", "Each paid bay's page tells you whether it's charged right now, and when that next changes.", palette)
        Benefit(Icons.Filled.Favorite, "Keeps the app going", "Pays for the map, the servers and the time spent chasing councils for bay data.", palette)

        if (state.isSupporter) {
            AuroraCard(palette, radius = 16, color = palette.searchSurface) {
                Text("Supporter unlocked", style = auroraStyle(13.sp, FontWeight.Bold, palette.searchIconActive), modifier = Modifier.testTag("supporter-unlocked"))
                VSpace(2)
                Text("Your purchase is tied to your Google account, so it comes back when you reinstall or change phone.", style = auroraStyle(11.sp, color = palette.cardSubtext))
            }
        } else {
            val price = state.price
            AuroraPrimaryButton(
                text = when {
                    state.pending -> "Payment pending…"
                    price != null -> "Become a Supporter · $price"
                    else -> "Not available right now"
                },
                palette = palette,
                enabled = price != null && !state.pending,
                onClick = {
                    val activity = context.findActivity()
                    if (activity == null || !app.supporter.buy(activity)) {
                        scope.launch { snackbar.showSnackbar("Couldn't open Google Play. Check you're online and try again.") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("supporter-buy"),
            )
            Text(
                when {
                    state.pending -> "Google Play is still confirming your payment. Supporter unlocks as soon as it goes through."
                    price != null -> "One-off payment through Google Play. No subscription, nothing to cancel."
                    else -> "Google Play couldn't be reached, or Supporter isn't on sale yet. Try again later."
                },
                style = auroraStyle(11.sp, color = palette.cardSubtext),
            )
            AuroraTextAction("Restore purchase", palette.searchIconActive, onClick = {
                app.supporter.refresh()
                scope.launch { snackbar.showSnackbar("Checking Google Play for an earlier purchase…") }
            })
        }

        if (BuildConfig.DEBUG) {
            AuroraTextAction(
                if (state.isSupporter) "Debug: turn Supporter off" else "Debug: turn Supporter on",
                palette.cardSubtext,
                onClick = { app.supporter.setForTesting(!state.isSupporter) },
            )
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String, palette: AuroraPalette) {
    AuroraCard(palette, radius = 16) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = palette.searchIconActive, modifier = Modifier.size(20.dp))
            HSpace(12)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = auroraStyle(13.sp, FontWeight.SemiBold, palette.cardTitleText))
                Text(text, style = auroraStyle(11.sp, color = palette.cardSubtext))
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
