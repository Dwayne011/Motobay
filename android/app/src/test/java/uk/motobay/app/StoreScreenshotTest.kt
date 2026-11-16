package uk.motobay.app

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.ui.MotoBayApp
import uk.motobay.app.ui.explore.LocalMapSnapshot
import uk.motobay.app.ui.explore.MapSnapshot
import uk.motobay.app.ui.theme.MotoBayTheme

/**
 * The Google Play phone screenshots: 1080 x 1920 (9:16), the real screens with the real
 * bay file, over a picture of the real map around Bloomsbury rendered by
 * scripts/store/render_map.mjs at the same camera. No made-up ratings or comments.
 * Writes PNGs to app/build/outputs/store.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi", application = TestMotoBayApplication::class)
class StoreScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<MotoBayApplication>()

    @Before
    fun seed() {
        // The real bay file from the repository (the app downloads the same file).
        app.bays.testDataset = File("../../data/uk-bays.json").readText()
        runBlocking { app.bays.load() }
        app.user.updateSettings { it.copy(displayName = "Rider", themeMode = ThemeMode.Light, disclaimerAccepted = true) }
        // An ordinary rider, so there's no moderator tab.
        (app.auth as FakeAuthRepository).set(FakeAuthRepository.RIDER)
        SAVED.forEach { id -> app.user.toggleFavourite(app.bays.bays.value.getValue(id)) }
    }

    private fun setApp(dark: Boolean) {
        app.user.updateSettings { it.copy(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) }
        val image = BitmapFactory.decodeStream(javaClass.getResourceAsStream("/store/map-${if (dark) "dark" else "light"}.jpg")).asImageBitmap()
        val snapshot = MapSnapshot(image, lat = 51.5205, lon = -0.1235, zoom = 15.0)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalMapSnapshot provides snapshot) {
                val settings by app.user.settings.collectAsStateWithLifecycle()
                MotoBayTheme(settings) { MotoBayApp(app) }
            }
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/store/$name.png")

    /** Opens Russell Square's preview card through search > Saved, as a rider would. */
    private fun pickRussellSquare() {
        compose.onNodeWithText("Search a town, street or postcode").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Saved")[0].performClick()
        compose.onAllNodesWithText("Russell Square")[0].performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("bay-preview").assertExists()
    }

    @Test fun store_light() {
        setApp(dark = false)
        shot("1-explore-map")
        pickRussellSquare()
        compose.onNodeWithTag("bay-preview").performClick()
        compose.waitForIdle()
        shot("3-bay-page")
        compose.onNodeWithText("Saved").performClick()
        compose.waitForIdle()
        shot("4-saved")
    }

    @Test fun store_dark() {
        setApp(dark = true)
        pickRussellSquare()
        shot("2-explore-preview-dark")
    }

    @Test fun store_list() {
        setApp(dark = false)
        compose.onAllNodesWithText("List", substring = true)[0].performClick()
        compose.waitForIdle()
        shot("5-explore-list")
    }

    private companion object {
        val SAVED = listOf(
            "node/6525047407", // Russell Square, 10 spaces
            "node/3831143377", // Red Lion Square
            "node/6524987944", // Thornhaugh Street, free
            "node/1700009647", // New Oxford Street
            "node/6877640744", // Russell Square, price unknown
        )
    }
}
