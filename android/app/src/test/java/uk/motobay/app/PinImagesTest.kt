package uk.motobay.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.motobay.app.data.Fee
import uk.motobay.app.ui.explore.PIN_DP
import uk.motobay.app.ui.explore.PinColors
import uk.motobay.app.ui.explore.PinImages
import java.io.File
import androidx.compose.ui.graphics.Color as ComposeColor

/** Renders the marker bitmaps the map uses and saves a contact sheet to app/build/outputs/roborazzi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "xxhdpi")
class PinImagesTest {
    private val colors = PinColors(
        free = ComposeColor(0xFF2E7D32), onFree = ComposeColor.White,
        paid = ComposeColor(0xFFFFB300), onPaid = ComposeColor(0xFF3A2A00),
        unknown = ComposeColor(0xFFD81B60), onUnknown = ComposeColor.White,
        selectedRing = ComposeColor.Black,
        cluster = ComposeColor(0xFFBBDEFB), onCluster = ComposeColor(0xFF0D47A1),
        stroke = ComposeColor.White, user = ComposeColor.Blue,
    )
    private val images = PinImages(ApplicationProvider.getApplicationContext(), colors)

    @Test fun pinIsSizedInDpAndFilledWithTheFeeColour() {
        val pin = images[images.pinKey(Fee.Unknown)]
        val density = 3f // xxhdpi
        assertEquals((PIN_DP * density).toInt(), pin.bitmap.width)
        assertEquals(480, pin.bitmap.density)
        // The pin's tail, below the glyph, is the fee colour; the tip anchor is inside the bitmap.
        // (Path units map to pixels as unit * PIN_DP/24 * density, shifted down by the outline and up by the 2-unit top margin.)
        val unit = PIN_DP / 24f * density
        assertEquals(0xFFD81B60.toInt(), pin.bitmap.getPixel((12f * unit).toInt(), (11f * unit + 1.5f * density).toInt()))
        assertTrue(pin.anchorY < pin.bitmap.height && pin.anchorY > pin.bitmap.height * 0.9f)
    }

    @Test fun clusterShowsItsCount() {
        val c = images[images.clusterKey(1234)]
        assertEquals("cluster/1k/32", images.clusterKey(1234))
        assertEquals(c.bitmap.width / 2f, c.anchorX)
        assertEquals(0xFFBBDEFB.toInt(), c.bitmap.getPixel(8, c.bitmap.height / 2))
    }

    @Test fun contactSheet() {
        val keys = listOf(
            images.pinKey(Fee.Free), images.pinKey(Fee.Paid), images.pinKey(Fee.Unknown),
            images.pinKey(Fee.Unknown, selected = true), images.clusterKey(3), images.clusterKey(42), images.clusterKey(250), images.clusterKey(1234),
        )
        val bitmaps = keys.map { images[it].bitmap }
        val sheet = Bitmap.createBitmap(bitmaps.sumOf { it.width + 24 }, bitmaps.maxOf { it.height } + 16, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(40, 44, 52))
        var x = 12f
        for (b in bitmaps) { canvas.drawBitmap(b, x, (sheet.height - b.height) / 2f, null); x += b.width + 24 }
        val out = File("build/outputs/roborazzi/markers.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
