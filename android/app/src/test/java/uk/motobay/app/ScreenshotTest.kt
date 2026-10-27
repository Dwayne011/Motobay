package uk.motobay.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.AuthUser
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.Fee
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
@Config(sdk = [35], application = TestMotoBayApplication::class)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<MotoBayApplication>()

    @Before
    fun seed() {
        app.user.updateSettings { it.copy(displayName = "Dwayne", themeMode = ThemeMode.Light, disclaimerAccepted = true) }
        // Signed in as the admin, so the Review tab shows.
        (app.auth as FakeAuthRepository).set(FakeAuthRepository.ADMIN)
        runBlocking {
            app.community.submitComment(SampleBays.all[0], "Usually has space before 9am.", "Sam")
            app.community.moderateComment(app.community.data.value.comments.first().id, ModerationStatus.Approved)
            app.community.submitComment(SampleBays.all[1], "Pay by app only.", "Alex")
        }
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
        compose.onNodeWithContentDescription("Comments, 1 waiting").performClick()
        compose.onNodeWithText("Pay by app only.").assertExists()
        shot("phone-review")
        compose.onNodeWithText("Approve").performClick()
        compose.waitForIdle()
        assertEquals(ModerationStatus.Approved, app.community.data.value.comments.first().status)
        // Approved items stay reachable so the moderator can delete them.
        compose.onNodeWithText("Approved").performClick()
        compose.onNodeWithText("Pay by app only.").assertExists()
        compose.onAllNodesWithContentDescription("Delete")[0].performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertTrue(app.community.data.value.comments.none { it.text == "Pay by app only." })
        compose.onNodeWithText("Settings").performClick()
        shot("phone-settings")
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_review_bays_tab() {
        runBlocking {
            app.community.submitBay(BaySubmission("", null, 51.52, -0.13, "Behind the station", Fee.Free, 6, true, "Next to the bins", "Sam", 0, ModerationStatus.Pending))
            app.community.submitBay(BaySubmission("", SampleBays.all[0].id, 51.5136, -0.1365, "", Fee.Paid, null, null, "", "Alex", 0, ModerationStatus.Pending))
        }
        setApp()
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithContentDescription("Bays, 2 waiting").performClick()
        compose.onNodeWithText("New bay").assertExists()
        compose.onNodeWithText("Edit: Sample bay 1").assertExists()
        shot("phone-review-bays")
    }

    /** A rejection with a reason reaches the rider on their My submissions page. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_reject_with_reason_shows_in_my_submissions() {
        // A rider posts, then the admin rejects with a reason.
        val auth = app.auth as FakeAuthRepository
        auth.set(FakeAuthRepository.RIDER)
        runBlocking { app.community.submitComment(SampleBays.all[0], "Cheap all day", "Rider") }
        auth.set(FakeAuthRepository.ADMIN)
        setApp()
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithContentDescription("Comments, 2 waiting").performClick()
        compose.onNodeWithText("Cheap all day").assertExists()
        // Newest first, so the rider's comment is the top card.
        compose.onAllNodesWithText("Reject")[0].performClick()
        compose.onNodeWithText("Reject this comment?").assertExists()
        compose.onNodeWithText("Reason (optional)").performTextInput("Bay is paid after 9am")
        compose.onNodeWithTag("reject-confirm").performClick()
        compose.waitForIdle()
        val comment = app.community.data.value.comments.first { it.text == "Cheap all day" }
        assertEquals(ModerationStatus.Rejected, comment.status)
        assertEquals("Bay is paid after 9am", comment.reason)

        // The rider sees it, with the reason.
        auth.set(FakeAuthRepository.RIDER)
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("My submissions").performClick()
        compose.onNodeWithContentDescription("Rejected, 1").performClick()
        compose.onNodeWithText("Reason: Bay is paid after 9am").assertExists()
        shot("phone-my-submissions")
        // Search narrows the list; a miss says so.
        compose.onNodeWithText("Search bay, text or reason").performTextInput("zzz")
        compose.onNodeWithText("Nothing rejected matches \"zzz\".").assertExists()
        compose.onNodeWithText("zzz").performTextClearance()
        compose.onNodeWithText("Search bay, text or reason").performTextInput("cheap")
        // Tapping a row opens the details with the full reason and a way back to the bay.
        compose.onNodeWithText("Reason: Bay is paid after 9am").performClick()
        compose.onNodeWithText("Not approved: Bay is paid after 9am").assertExists()
        compose.onNodeWithText("Open bay and try again").assertExists()
    }

    /** Names only change on Save, and a name another rider holds is refused. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_display_name_saves_and_rejects_duplicates() {
        val names = app.displayNames as FakeDisplayNames
        names.taken["ridermike"] = "someoneElse" to "RiderMike"
        app.user.updateSettings { it.copy(displayName = "") }
        setApp()
        compose.onNodeWithText("Settings").performClick()
        // Typing shows availability live, and a taken name can't be saved.
        compose.onNodeWithTag("display-name").performTextInput("RiderMike")
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("Someone already has that name").assertExists()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        assertEquals("", app.user.settings.value.displayName)
        compose.onNodeWithTag("display-name").performTextClearance()
        compose.onNodeWithTag("display-name").performTextInput("Dwayne")
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("✓ Available").assertExists()
        assertEquals("", app.user.settings.value.displayName) // typing alone changes nothing
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertEquals("Dwayne", app.user.settings.value.displayName)
        assertEquals("admin1" to "Dwayne", names.taken["dwayne"])
        // Saved: the field locks and offers Edit.
        compose.onNodeWithText("Edit").assertExists()
        shot("phone-settings-name-saved")
    }

    /** First launch shows the guidance-only notice once; accepting it is remembered. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_first_launch_disclaimer() {
        app.user.updateSettings { it.copy(disclaimerAccepted = false) }
        setApp()
        compose.onNodeWithText("Before you ride").assertExists()
        compose.onNodeWithText("I understand").performClick()
        compose.waitForIdle()
        assertTrue(app.user.settings.value.disclaimerAccepted)
        compose.onNodeWithText("Before you ride").assertDoesNotExist()
    }

    /** The privacy policy is readable inside the app, from the bundled copy of docs/privacy.md. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_privacy_policy_in_app() {
        setApp()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()
        compose.onNodeWithText("MotoBays Privacy Policy").assertExists()
        compose.onNodeWithText("1. Who we are").assertExists()
        shot("phone-privacy-policy")
    }

    /** Legal screen lists the licences and the privacy policy. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_legal_screen() {
        setApp()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Legal & about").performScrollTo().performClick()
        compose.onNodeWithText("Privacy policy").assertExists()
        compose.onNodeWithText("Bay locations").assertExists()
        shot("phone-legal")
    }

    /** Deleting the account removes the rider's content, name and sign-in. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_delete_account_wipes_everything() {
        val auth = app.auth as FakeAuthRepository
        val names = app.displayNames as FakeDisplayNames
        names.taken["dwayne"] = "admin1" to "Dwayne"
        runBlocking {
            auth.set(FakeAuthRepository.RIDER)
            app.community.submitComment(SampleBays.all[1], "Someone else's comment", "Rider")
            auth.set(FakeAuthRepository.ADMIN)
            app.community.submitComment(SampleBays.all[1], "Mine to delete", "Dwayne")
        }
        setApp()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Delete account & data").performScrollTo().performClick()
        compose.onNodeWithText("Delete your account?").assertExists()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertTrue(auth.deleted)
        assertEquals(null, auth.user.value)
        assertTrue(app.community.data.value.comments.none { it.text == "Mine to delete" })
        assertTrue(app.community.data.value.comments.any { it.text == "Someone else's comment" }) // other riders' content stays
        assertEquals(null, names.taken["dwayne"])
        assertEquals("", app.user.settings.value.displayName)
    }

    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_settings_large_text_dark() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        setApp(dark = true)
        compose.onNodeWithText("Settings").performClick()
        shot("phone-settings-large-text-dark")
    }

    /** UGC: a rider can report and block another rider from a comment; blocking hides their content at once. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_report_and_block_comment() {
        val auth = app.auth as FakeAuthRepository
        auth.set(FakeAuthRepository.RIDER)
        runBlocking {
            app.community.submitComment(SampleBays.all[0], "Rude remark", "Rider")
            app.community.moderateComment(app.community.data.value.comments.first { it.text == "Rude remark" }.id, ModerationStatus.Approved)
        }
        auth.set(AuthUser("viewer1", "viewer@example.com", isModerator = false))
        app.user.updateSettings { it.copy(themeMode = ThemeMode.Light) }
        compose.setContent { DetailHost() }
        compose.onNodeWithText("Rude remark").assertExists()
        compose.onNodeWithContentDescription("More options for Rider's comment").performClick()
        compose.onNodeWithText("Report").performClick()
        compose.onNodeWithText("Abusive or offensive").performClick()
        compose.onNodeWithTag("report-send").performClick()
        compose.waitForIdle()
        val report = (app.community as FakeCommunityRepository).reports.single()
        assertEquals("comment", report["targetType"])
        assertEquals("rider1", report["targetAuthorUid"])
        assertEquals("viewer1", report["reporterUid"])
        assertEquals("abusive", report["reason"])

        compose.onNodeWithContentDescription("More options for Rider's comment").performClick()
        compose.onNodeWithText("Block Rider").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Rude remark").assertDoesNotExist()
        assertEquals(listOf("rider1"), app.user.settings.value.blocked.map { it.uid })
    }

    /** The tab counts must stay inside their segments at the largest common font size. */
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone_review_large_text() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        runBlocking { app.community.submitPhoto(SampleBays.all[0], android.net.Uri.parse("file:///none.jpg"), "", "Sam") }
        setApp()
        compose.onNodeWithText("Review").performClick()
        shot("phone-review-large-text")
        val row = compose.onNodeWithContentDescription("Comments, 1 waiting").fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithContentDescription("Comments, 1 waiting").fetchSemanticsNode()
            .let { node -> compose.onAllNodesWithText("1").fetchSemanticsNodes().first { it.boundsInRoot.left >= row.left - 1f && it.boundsInRoot.right <= row.right + 1f }.boundsInRoot }
        assertTrue("badge $badge must sit inside its segment $row", badge.right <= row.right && badge.left >= row.left)
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
        // "All" is always on screen; the pill row scrolls sideways, so later pills may be clipped.
        val chip = compose.onNodeWithText("All").fetchSemanticsNode().boundsInRoot
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
        // Signed in as the moderator, so approved comments can be deleted from here too.
        compose.onNodeWithText("Usually has space before 9am.").assertExists()
        compose.onNodeWithContentDescription("Delete comment").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertTrue(app.community.data.value.comments.none { it.text == "Usually has space before 9am." })
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

/** The Map / List switch on its own, at the tester's font size, with the widest count it can show. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = TestMotoBayApplication::class)
class ViewToggleScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun view_toggle_large_text() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        val app = ApplicationProvider.getApplicationContext<MotoBayApplication>()
        app.user.updateSettings { it.copy(themeMode = ThemeMode.Dark) }
        compose.setContent {
            val settings by app.user.settings.collectAsStateWithLifecycle()
            MotoBayTheme(settings) {
                Surface {
                    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                        uk.motobay.app.ui.explore.ViewToggle(uk.motobay.app.ui.explore.CompactView.Map, "5", {})
                        uk.motobay.app.ui.explore.ViewToggle(uk.motobay.app.ui.explore.CompactView.List, "200+", {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/view-toggle-large-text.png")
        // The segments share one width, so the toggle must grow for the wider count rather than clip it.
        val narrow = compose.onAllNodesWithTag("view-toggle")[0].fetchSemanticsNode().boundsInRoot
        val wide = compose.onAllNodesWithTag("view-toggle")[1].fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("List\u00A0200+").assertExists()
        assertTrue("toggle must widen for a 3-digit count ($narrow vs $wide)", wide.width > narrow.width + 40f)
    }
}
