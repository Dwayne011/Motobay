package uk.motobay.app

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import uk.motobay.app.ui.explore.ExploreViewModel

/** Going to the rider when the app starts: once per launch, asking for permission only the first time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMotoBayApplication::class)
class LaunchLocationTest {
    private val app get() = ApplicationProvider.getApplicationContext<MotoBayApplication>()

    @Test fun asksOnceOnTheFirstLaunchAfterTheNoticeIsRead() {
        val vm = ExploreViewModel(app)
        // Not before the opening notice has been accepted.
        assertFalse(vm.centreOnLaunch())
        app.user.updateSettings { it.copy(disclaimerAccepted = true) }
        assertTrue(vm.centreOnLaunch())
        assertTrue(app.user.settings.value.locationAsked)
        // Not again this launch, and not on a later launch either.
        assertFalse(vm.centreOnLaunch())
        app.centredOnLaunch = false
        assertFalse(vm.centreOnLaunch())
    }

    @Test fun withLocationAllowedItGoesStraightThereWithoutAsking() {
        shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
        app.user.updateSettings { it.copy(disclaimerAccepted = true) }
        val vm = ExploreViewModel(app)
        assertFalse(vm.centreOnLaunch())
        assertTrue(app.centredOnLaunch)
        assertFalse(app.user.settings.value.locationAsked)
    }
}
