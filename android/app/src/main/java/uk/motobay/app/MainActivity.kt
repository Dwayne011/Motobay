package uk.motobay.app

import android.content.Intent
import android.graphics.Color
import uk.motobay.app.alerts.ChargeAlerts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.ui.MotoBayApp
import uk.motobay.app.ui.explore.LocalMapHolder
import uk.motobay.app.ui.explore.MapHolder
import uk.motobay.app.ui.theme.MotoBayTheme
import uk.motobay.app.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {
    /** One map for the whole activity, paused rather than destroyed between screens (see MapHolder). */
    private val mapHolder by lazy { MapHolder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MotoBayApplication
        if (savedInstanceState == null) takeBayRequest(intent)
        setContent {
            CompositionLocalProvider(LocalMapHolder provides mapHolder) {
            val settings by app.user.settings.collectAsStateWithLifecycle()
            val dark = isAppInDarkTheme(settings.themeMode)
            // Keep status and navigation bar icons readable when the in-app theme differs from the system's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            MotoBayTheme(settings) {
                MotoBayApp(app)
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeBayRequest(intent)
    }

    /** A charge alert or the widget asks for a bay to be shown. */
    private fun takeBayRequest(intent: Intent?) {
        intent?.getStringExtra(ChargeAlerts.EXTRA_BAY_ID)?.let { (application as MotoBayApplication).openBayRequest.value = it }
    }

    override fun onDestroy() {
        mapHolder.destroy()
        super.onDestroy()
    }
}
