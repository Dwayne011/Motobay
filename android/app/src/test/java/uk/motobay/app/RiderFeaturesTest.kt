package uk.motobay.app

import android.app.AlarmManager
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayFeature
import uk.motobay.app.data.Fee
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.data.epochMillis
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.theme.MotoBayTheme
import uk.motobay.app.ui.theme.auroraPalette
import uk.motobay.app.ui.theme.frostBackdrop
import java.time.LocalDateTime

/** "I parked here", the RingGo button, space reports and safety details on the bay page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = TestMotoBayApplication::class)
class RiderFeaturesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<TestMotoBayApplication>()
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))

    private val islington = Bay(
        id = "council/islington/aberdeen-road-54137", lat = 51.554299, lon = -0.095052, name = "Motorcycle parking",
        fee = Fee.Paid, street = "Aberdeen Road", area = "Highbury", postcode = "N5", capacity = 6, charged = "1-5 08:30-18:30",
        rules = "Paid Mon–Fri 8.30am–6.30pm; free outside those hours. Pay by RingGo, location 54137. Controlled zone Q.",
        source = "Islington Council motorcycle bay list (FOI 8652875)",
    )

    @Before
    fun seed() {
        app.user.updateSettings { it.copy(displayName = "Sam", themeMode = ThemeMode.Light, disclaimerAccepted = true) }
        app.user.setParked(null)
        shadowOf(app as Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test fun parking_before_charging_starts_sets_a_reminder() {
        app.fixedNow = LocalDateTime.of(2026, 11, 10, 7, 0) // Tuesday 7am, free until 8.30
        compose.setContent { Host(islington) }
        compose.onNodeWithTag("park-here").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(islington.id, app.user.parked.value?.bayId)
        compose.onNodeWithText("Free until 8.30am. We'll remind you 15 minutes before charging starts.").assertExists()
        val alarm = alarms.scheduledAlarms.single()
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 15).epochMillis(), alarm.triggerAtMs)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-parked-here.png")

        // "I've left" clears it and the alarm.
        compose.onNodeWithTag("left-bay").performScrollTo().performClick()
        compose.waitForIdle()
        assertNull(app.user.parked.value)
        assertNull(alarms.nextScheduledAlarm)
        compose.onNodeWithTag("park-here").assertExists()
    }

    @Test fun parking_while_charged_says_so_and_sets_no_reminder() {
        app.fixedNow = LocalDateTime.of(2026, 11, 10, 10, 30)
        compose.setContent { Host(islington) }
        compose.onNodeWithTag("park-here").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Charged now, free from 6.30pm. Pay now if you haven't.").assertExists()
        assertNull(alarms.nextScheduledAlarm)
    }

    @Test fun the_reminder_names_the_bay_and_the_time() {
        app.fixedNow = LocalDateTime.of(2026, 11, 10, 8, 15)
        app.user.setParked(uk.motobay.app.data.Parked(islington.id, islington.title, islington.lat, islington.lon, Fee.Paid, islington.charged, 0))
        uk.motobay.app.alerts.ChargeAlertReceiver().onReceive(app, android.content.Intent())
        val n = shadowOf(app.getSystemService(android.app.NotificationManager::class.java)).allNotifications.single()
        assertEquals("Aberdeen Road", shadowOf(n).contentTitle)
        assertEquals("Charging starts at 8.30am. Pay or move your bike.", shadowOf(n).contentText)
    }

    @Test fun pay_button_copies_the_location_code() {
        compose.setContent { Host(islington) }
        compose.onNodeWithTag("pay-button").performScrollTo().assertExists()
        compose.onNodeWithText("Pay with RingGo · location 54137").performClick()
        val clip = app.getSystemService(android.content.ClipboardManager::class.java).primaryClip
        assertEquals("54137", clip?.getItemAt(0)?.text)
        // RingGo isn't installed in the test, so its website opens.
        val opened = shadowOf(app as Application).nextStartedActivity
        assertEquals("https://myringgo.co.uk/", opened?.dataString)
    }

    @Test fun a_free_bay_has_no_pay_button() {
        compose.setContent { Host(islington.copy(fee = Fee.Free, rules = null, charged = null)) }
        compose.onNodeWithTag("pay-button").assertDoesNotExist()
    }

    @Test fun riders_report_space_and_safety_and_everyone_sees_the_totals() {
        app.fixedNow = LocalDateTime.of(2026, 11, 10, 10, 30)
        val fake = app.bayStatus as FakeBayStatusRepository
        fake.seedSpace(islington.id, "rider2", full = true, at = LocalDateTime.of(2026, 11, 10, 10, 10))
        fake.seedFeature(islington.id, "rider2", BayFeature.Anchor, true)
        compose.setContent { Host(islington) }
        compose.onNodeWithTag("space-status").performScrollTo()
        compose.onNodeWithText("Reported full 20 min ago").assertExists()
        compose.onNodeWithText("1 rider says yes").assertExists()

        // Signed out, the report is refused, so nothing changes.
        compose.onNodeWithText("Space now").performClick()
        compose.onNodeWithText("Reported full 20 min ago").assertExists()

        (app.auth as FakeAuthRepository).set(FakeAuthRepository.RIDER)
        compose.waitForIdle()
        compose.onNodeWithText("Space now").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Space reported just now").assertExists()
        compose.onNodeWithTag("feature-cctv").performScrollTo()
        compose.onNodeWithTag("feature-cctv").assertTextEquals("Not known yet")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-bay-status.png")
    }

    @Composable
    private fun Host(bay: Bay) {
        val settings by app.user.settings.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalInspectionMode provides true, LocalApp provides app, LocalSnackbar provides SnackbarHostState()) {
            MotoBayTheme(settings) {
                Box(Modifier.fillMaxSize().frostBackdrop(auroraPalette())) { BayDetailContent(bay) }
            }
        }
    }
}
