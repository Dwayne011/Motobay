package uk.motobay.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.data.PlaceSearch
import uk.motobay.app.data.SampleBays
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.MotoBayApp
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.theme.MotoBayTheme

/**
 * Renders the real Compose screens on the JVM and saves PNGs to
 * app/build/outputs/roborazzi. The map itself is a placeholder here because
 * MapLibre needs a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<MotoBayApplication>()

    @Before
    fun seed() {
        app.user.updateSettings { it.copy(moderator = true, displayName = "Dwayne", themeMode = ThemeMode.Light) }
        app.community.submitComment(SampleBays.all[0], "Usually has space before 9am.", "Sam")
        app.community.moderateComment(app.community.data.value.comments.first().id, ModerationStatus.Approved)
        app.community.submitComment(SampleBays.all[1], "Pay by app only.", "Alex")
        app.user.toggleFavourite(SampleBays.all[0])
    }

    private fun setApp(dark: Boolean = false) {
        app.user.updateSettings { it.copy(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) }
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                val settings by app.user.settings.collectAsStateWithLifecycle()
                MotoBayTheme(settings) { MotoBayApp(app) }
            }
        }
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_explore_saved_review_settings() {
        setApp()
        shot("phone-explore")
        compose.onNodeWithText("Saved").performClick()
        compose.onNodeWithText("Sample bay 1").assertExists()
        shot("phone-saved")
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithText("Comments (1)").performClick()
        compose.onNodeWithText("Pay by app only.").assertExists()
        shot("phone-review")
        compose.onNodeWithText("Approve").performClick()
        compose.waitForIdle()
        assertEquals(ModerationStatus.Approved, app.community.data.value.comments.first().status)
        compose.onNodeWithText("Settings").performClick()
        shot("phone-settings")
    }

    /** Uses a recorded Photon response, so it runs offline. */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_search_shows_suggestions_while_typing() {
        val photon = javaClass.classLoader!!.getResource("photon-camden.json").readText()
        app.placeSearch = PlaceSearch(fetch = { photon })
        setApp()
        compose.onNodeWithText("Search a town, street or postcode").performClick()
        compose.waitForIdle()
        shot("phone-search-empty")
        compose.onNodeWithText("Search a town, street or postcode").performTextInput("camden")
        // Robolectric's clock only moves when told to, so step it past the typing debounce.
        compose.waitUntil(timeoutMillis = 15_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(300, java.util.concurrent.TimeUnit.MILLISECONDS)
            compose.onAllNodesWithText("Camden Town").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        shot("phone-search-camden")
    }

    /** Large text (like the tester's phone) must not push the filters under the search bar. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_explore_large_text_dark() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        setApp(dark = true)
        shot("phone-explore-large-text-dark")
        val bar = compose.onNodeWithText("Search a town, street or postcode").fetchSemanticsNode().boundsInRoot
        val chip = compose.onNodeWithText("Covered").fetchSemanticsNode().boundsInRoot
        assertTrue("filters overlap the search bar", chip.top >= bar.bottom)
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_search_empty_state_and_add_bay() {
        // The map is a placeholder in tests, so give it a position to add the bay at.
        app.user.updateSettings { it.copy(lastMap = uk.motobay.app.data.CameraSnapshot(51.5277, -0.0815, 16.0)) }
        app.user.addRecentPlace(uk.motobay.app.data.RecentPlace("Hoxton Square", "London", 51.5277, -0.0815))
        setApp(dark = true)
        compose.onNodeWithText("Search a town, street or postcode").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Hoxton Square").assertExists()
        shot("phone-dark-search-recent")
        // The "Saved" chip (the navigation bar also has a "Saved" tab).
        compose.onAllNodesWithText("Saved")[0].performClick()
        compose.onNodeWithText("Sample bay 1").assertExists()
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.onNodeWithContentDescription("Add a bay").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Free").assertExists()
        shot("phone-dark-add-bay-form")
    }

    /** Tapping a bay opens a sheet over the map rather than a new screen. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_bay_opens_as_bottom_sheet() {
        app.user.updateSettings { it.copy(lastMap = uk.motobay.app.data.CameraSnapshot(51.5136, -0.1365, 16.0)) }
        setApp(dark = true)
        // The map is a placeholder in tests, so pick the bay from Saved in the search view.
        compose.onNodeWithText("Search a town, street or postcode").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Saved")[0].performClick()
        compose.onNodeWithText("Sample bay 1").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Directions").assertExists()
        // Still on Explore: the search bar is there, no "Bay details" app bar.
        compose.onNodeWithText("Search a town, street or postcode").assertExists()
        shot("phone-dark-bay-sheet")
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Directions").assertDoesNotExist()
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_dark_settings() {
        setApp(dark = true)
        compose.onNodeWithText("Settings").performClick()
        shot("phone-dark-settings")
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet_explore_uses_navigation_rail() {
        setApp()
        shot("tablet-explore")
        compose.onNodeWithText("Explore").assertExists()
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_bay_detail() {
        app.user.updateSettings { it.copy(themeMode = ThemeMode.Light) }
        compose.setContent { DetailHost() }
        compose.onNodeWithText("Directions").assertExists()
        shot("phone-bay-detail")
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_bay_detail_dark_and_comment_goes_to_review() {
        app.user.updateSettings { it.copy(themeMode = ThemeMode.Dark) }
        compose.setContent { DetailHost() }
        shot("phone-dark-bay-detail")
        val before = app.community.data.value.comments.size
        compose.onNodeWithText("Add a comment").performClick()
        compose.onNodeWithText("Add a comment").performTextInput("Two spaces free at lunchtime.")
        compose.onNodeWithText("Post").performClick()
        compose.waitForIdle()
        val comments = app.community.data.value.comments
        assertEquals(before + 1, comments.size)
        assertTrue(comments.first().status == ModerationStatus.Pending)
    }

    @Composable
    private fun DetailHost() {
        val settings by app.user.settings.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalInspectionMode provides true, LocalApp provides app, LocalSnackbar provides SnackbarHostState()) {
            MotoBayTheme(settings) { Surface { BayDetailContent(SampleBays.all[0]) } }
        }
    }
}
