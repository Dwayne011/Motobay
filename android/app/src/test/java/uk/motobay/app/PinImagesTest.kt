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
import uk.motobay.app.ui.explore.MARKER_DP
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
        free = ComposeColor(0xFF22C55E),
        paid = ComposeColor(0xFFF59E0B),
        unknown = ComposeColor(0xFFEF4444),
        pillFill = ComposeColor(0xFF14171C),
        pillBorder = ComposeColor(0xFF475569),
        pillText = ComposeColor.White,
        selectedFill = ComposeColor(0xFF1D2228),
        stroke = ComposeColor.White, user = ComposeColor.Blue,
    )
    private val images = PinImages(ApplicationProvider.getApplicationContext(), colors)

    @Test fun pillIsSizedInDpWithItsStatusDotAndBorder() {
        val pin = images[images.pinKey(Fee.Unknown)]
        val density = 3f // xxhdpi
        // 32dp tall plus the 1.5dp border top and bottom; at least the 44dp touch target wide; anchored at the centre.
        assertEquals(((MARKER_DP + 3f) * density).toInt(), pin.bitmap.height)
        assertTrue(pin.bitmap.width >= (44f * density).toInt())
        assertEquals(480, pin.bitmap.density)
        assertEquals(pin.bitmap.width / 2f, pin.anchorX)
        assertEquals(pin.bitmap.height / 2f, pin.anchorY)
        // Border colour on the edge, fill colour just inside it, and the coral dot somewhere on the centre line.
        assertEquals(0xFF475569.toInt(), pin.bitmap.getPixel(pin.bitmap.width / 2, 1))
        assertEquals(0xFF14171C.toInt(), pin.bitmap.getPixel(pin.bitmap.width / 2, (3f * density).toInt()))
        val mid = pin.bitmap.height / 2
        assertTrue((0 until pin.bitmap.width).any { pin.bitmap.getPixel(it, mid) == 0xFFEF4444.toInt() })
        // The selected pill takes the status colour as its border.
        val selected = images[images.pinKey(Fee.Unknown, selected = true)]
        assertEquals(0xFFEF4444.toInt(), selected.bitmap.getPixel(selected.bitmap.width / 2, 1))
    }

    @Test fun clusterShowsItsCount() {
        val c = images[images.clusterKey(1234)]
        assertEquals("cluster/1k", images.clusterKey(1234))
        assertEquals(c.bitmap.width / 2f, c.anchorX)
        assertEquals(0xFF14171C.toInt(), c.bitmap.getPixel(8, c.bitmap.height / 2))
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
