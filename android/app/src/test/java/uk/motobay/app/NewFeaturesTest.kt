package uk.motobay.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.data.CameraSnapshot
import uk.motobay.app.data.Fee
import uk.motobay.app.data.FuelGrade
import uk.motobay.app.data.LatLon
import uk.motobay.app.data.SampleBays
import uk.motobay.app.data.ThemeMode
import uk.motobay.app.data.cheapestNear
import uk.motobay.app.data.parseFuel
import uk.motobay.app.data.tileCount
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar
import uk.motobay.app.ui.bay.BayDetailContent
import uk.motobay.app.ui.explore.Destination
import uk.motobay.app.ui.explore.nearestTo
import uk.motobay.app.ui.explore.walkingMinutes
import uk.motobay.app.ui.settings.OfflineMapsPage
import uk.motobay.app.ui.theme.MotoBayTheme
import uk.motobay.app.ui.theme.auroraPalette
import uk.motobay.app.ui.theme.frostBackdrop
import java.time.LocalDateTime

/** Council-checked badge, suspended reports, parking near a destination, fuel and offline maps. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", application = TestMotoBayApplication::class)
class NewFeaturesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<TestMotoBayApplication>()

    private val islington = Bay(
        id = "council/islington/aberdeen-road-54137", lat = 51.554299, lon = -0.095052, name = "Motorcycle parking",
        fee = Fee.Paid, street = "Aberdeen Road", charged = "1-5 08:30-18:30",
        rules = "Paid Mon–Fri 8.30am–6.30pm; free outside those hours. Pay by RingGo, location 54137.",
        source = "Islington Council motorcycle bay list (FOI 8652875)",
    )

    // Two forecourts near Soho (Sample bay 1) and one far away.
    private val fuelJson = """
        {"generated":"2026-11-19T01:00:00Z","stations":[
          ["a","Cheap Fuel","ACME",51.5140,-0.1360,"W1D 1AA",139.9,152.9,149.9,"2026-11-19T00:30:00",false],
          ["b","Dear Fuel",null,51.5150,-0.1370,"W1D 2BB",145.9,null,null,null,false],
          ["c","Far Fuel","ACME",52.5,-1.9,"B1 1AA",120.9,130.9,null,null,true]
        ]}
    """.trimIndent()

    @Before fun seed() {
        app.user.updateSettings { it.copy(themeMode = ThemeMode.Light, disclaimerAccepted = true) }
        app.fixedNow = LocalDateTime.of(2026, 11, 10, 10, 30)
    }

    @Composable
    private fun Host(content: @Composable () -> Unit) {
        val settings by app.user.settings.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalInspectionMode provides true, LocalApp provides app, LocalSnackbar provides SnackbarHostState()) {
            MotoBayTheme(settings) { Box(Modifier.fillMaxSize().frostBackdrop(auroraPalette())) { content() } }
        }
    }

    @Test fun council_bays_say_council_checked_and_osm_bays_do_not() {
        assertTrue(islington.isCouncilChecked)
        assertFalse(SampleBays.all[0].isCouncilChecked)
        assertFalse(islington.copy(source = "MotoBays rider survey (City of London, November 2026)").isCouncilChecked)
        compose.setContent { Host { BayDetailContent(islington) } }
        compose.onNodeWithTag("council-checked").assertExists()
    }

    @Test fun a_rider_can_report_a_bay_suspended() {
        (app.auth as FakeAuthRepository).set(FakeAuthRepository.RIDER)
        compose.setContent { Host { BayDetailContent(islington) } }
        compose.onNodeWithTag("space-status").performScrollTo()
        compose.onNodeWithText("Suspended").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Reported suspended just now").assertExists()
    }

    @Test fun walking_time_allows_for_streets() {
        assertEquals(1, walkingMinutes(10.0))
        assertEquals(5, walkingMinutes(300.0)) // 300 m straight line, about 390 m of street
    }

    @Test fun parking_near_a_destination_lists_the_nearest_bays() {
        val near = nearestTo(SampleBays.all, LatLon(51.5136, -0.1365))
        assertEquals("sample/1", near.first().bay.id)
        assertTrue(near.size <= 3 && near.all { (it.distanceMetres ?: 0.0) <= 1_000.0 })
        // Manchester and Edinburgh are far outside 1 km.
        assertTrue(near.none { it.bay.id in setOf("sample/6", "sample/7") })
    }

    @Test fun fuel_is_cheapest_first_within_five_miles_for_the_chosen_grade() {
        val data = parseFuel(fuelJson)
        assertEquals(3, data.stations.size)
        val soho = LatLon(51.5136, -0.1365)
        assertEquals(listOf("a", "b"), cheapestNear(data.stations, soho, FuelGrade.E10).map { it.station.id })
        // Only one sells super unleaded nearby; the cheaper one far away isn't shown.
        assertEquals(listOf("a"), cheapestNear(data.stations, soho, FuelGrade.E5).map { it.station.id })
        assertEquals("Acme Cheap Fuel", data.stations[0].title)
        // Fuel Finder's names come in capitals.
        val tesco = data.stations[0].copy(name = "HARROGATE - PETROL FILLING STATION", brand = "TESCO")
        assertEquals("Tesco Harrogate", tesco.title)
        assertEquals("BP Kings Cross", tesco.copy(name = "BP KINGS CROSS", brand = "BP").title)
        assertEquals("Asda Handbridge Express Petrol", tesco.copy(name = "ASDA HANDBRIDGE EXPRESS PETROL", brand = "ASDA HANDBRIDGE EXPRESS PETROL").title)
    }

    @Test fun the_fuel_chip_shows_the_cheapest_forecourts() {
        app.fuel.testDataset = fuelJson
        app.user.updateSettings { it.copy(lastMap = CameraSnapshot(51.5136, -0.1365, 15.0)) }
        compose.setContent {
            val settings by app.user.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalInspectionMode provides true) { MotoBayTheme(settings) { uk.motobay.app.ui.MotoBayApp(app) } }
        }
        compose.onNodeWithTag("layers-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Fuel prices").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("fuel-card").assertExists()
        compose.onNodeWithText("139.9p").assertExists()
        compose.onNodeWithText("E5").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("152.9p").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-fuel.png")
    }

    @Test fun offline_maps_saves_the_area_last_shown_and_refuses_a_huge_one() {
        val small = BBox(51.50, -0.15, 51.53, -0.10)
        assertTrue(tileCount(small) < 200)
        assertTrue(tileCount(BBox(50.0, -5.0, 55.0, 1.0)) > 3_000)
        app.user.updateSettings { it.copy(lastMap = CameraSnapshot(51.515, -0.125, 14.0, small.south, small.west, small.north, small.east)) }
        runBlocking { app.bays.load() }
        compose.setContent { Host { androidx.compose.foundation.layout.Column { OfflineMapsPage(auroraPalette()) } } }
        compose.onNodeWithTag("offline-download").assertIsEnabled().performClick()
        compose.waitForIdle()
        val fake = app.offlineAreas as FakeOfflineAreas
        assertEquals(small, fake.downloads.single().second)
        compose.onNodeWithText("Delete").assertExists()

        app.user.updateSettings { it.copy(lastMap = CameraSnapshot(52.5, -2.0, 6.0, 50.0, -5.0, 55.0, 1.0)) }
        compose.waitForIdle()
        compose.onNodeWithTag("offline-download").assertIsNotEnabled()
    }

    @Test fun the_filter_row_fits_on_screen_and_the_rest_are_in_the_sheet() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        compose.setContent {
            val settings by app.user.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalInspectionMode provides true) { MotoBayTheme(settings) { uk.motobay.app.ui.MotoBayApp(app) } }
        }
        // Filters, Free, Paid and Free now all fit across the screen, even with large text.
        val width = compose.onRoot().fetchSemanticsNode().boundsInRoot.right
        for (n in listOf(compose.onNodeWithTag("filters-button"), compose.onNodeWithText("Free"), compose.onNodeWithText("Paid"), compose.onNodeWithText("Free now"))) {
            assertTrue(n.fetchSemanticsNode().boundsInRoot.right <= width)
        }
        compose.onNodeWithText("Vehicle crime").assertDoesNotExist()
        compose.onNodeWithText("Covered").assertDoesNotExist()

        // Tapping Free filters; tapping it again goes back to every bay.
        compose.onNodeWithText("Free").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Free").assertIsSelected()
        compose.onNodeWithText("Free").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Free").assertIsNotSelected()

        // Hidden filters live in the sheet, and the button counts them.
        compose.onNodeWithTag("filters-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("filters-sheet").assertExists()
        compose.onNodeWithText("Covered only").performClick()
        compose.onNodeWithText("Unknown").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-filters-sheet.png")
        compose.onNodeWithTag("filters-show").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filters, 2 on").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-filter-row.png")

        // Clear all puts everything back.
        compose.onNodeWithTag("filters-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Clear all").performClick()
        compose.onNodeWithTag("filters-show").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filters").assertExists()
        org.robolectric.RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun the_layers_button_opens_fuel_and_crime() {
        compose.setContent {
            val settings by app.user.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalInspectionMode provides true) { MotoBayTheme(settings) { uk.motobay.app.ui.MotoBayApp(app) } }
        }
        compose.onNodeWithTag("layers-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("layers-sheet").assertExists()
        compose.onNodeWithText("Motorcycle garages").assertExists()
        compose.onNodeWithText("Fuel prices").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Super unleaded (E5)").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phone-layers-sheet.png")
    }
}
