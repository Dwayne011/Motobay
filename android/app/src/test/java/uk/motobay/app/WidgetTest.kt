package uk.motobay.app

import android.appwidget.AppWidgetManager
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import uk.motobay.app.data.Bay
import uk.motobay.app.data.Fee
import uk.motobay.app.data.LastFix
import uk.motobay.app.data.LatLon
import uk.motobay.app.widget.NearestFreeWidget
import uk.motobay.app.widget.nearestFree
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMotoBayApplication::class)
class WidgetTest {
    private val here = LatLon(51.5136, -0.1365)
    private val tue1030 = LocalDateTime.of(2026, 11, 10, 10, 30)
    private val tue1900 = LocalDateTime.of(2026, 11, 10, 19, 0)
    private fun bay(id: String, lat: Double, fee: Fee, charged: String? = null) =
        Bay(id, lat, -0.1365, "Motorcycle parking", fee, street = id, charged = charged)

    private val near = bay("Near Paid", 51.5140, Fee.Paid, "1-5 08:30-18:30") // about 50 yards
    private val further = bay("Further Free", 51.5160, Fee.Free) // about 290 yards

    @Test fun `everyone gets the nearest free bay`() {
        val r = nearestFree(listOf(near, further), here, tue1900, supporter = false)!!
        assertEquals("Further Free", r.bay.id)
        assertEquals("290 yd · free bay", r.detail)
    }

    @Test fun `Supporters also get paid bays outside their charging hours`() {
        val r = nearestFree(listOf(near, further), here, tue1900, supporter = true)!!
        assertEquals("Near Paid", r.bay.id)
        assertEquals("50 yd · free until tomorrow 8.30am", r.detail)
        // While it's charged, the free bay is nearest-free again.
        assertEquals("Further Free", nearestFree(listOf(near, further), here, tue1030, supporter = true)!!.bay.id)
    }

    @Test fun `nothing within 5 km means nothing to show`() {
        assertNull(nearestFree(listOf(bay("Far", 51.60, Fee.Free)), here, tue1030, supporter = false))
    }

    @Test fun `the widget shows the bay near the rider's last position`() {
        val app = ApplicationProvider.getApplicationContext<TestMotoBayApplication>()
        runBlocking { app.bays.load() } // the sample bays
        val widgets = shadowOf(AppWidgetManager.getInstance(app))
        val id = widgets.createWidget(NearestFreeWidget::class.java, R.layout.widget_nearest_free)
        val title = { widgets.getViewFor(id).findViewById<TextView>(R.id.widget_title).text.toString() }

        NearestFreeWidget.refresh(app)
        assertEquals("Open MotoBays to find bays near you", title())

        app.user.updateSettings { it.copy(lastFix = LastFix(51.5136, -0.1365)) } // at Sample bay 1
        NearestFreeWidget.refresh(app)
        assertEquals("Sample bay 1", title())
        assertEquals("0 yd · free bay", widgets.getViewFor(id).findViewById<TextView>(R.id.widget_detail).text.toString())
    }
}
