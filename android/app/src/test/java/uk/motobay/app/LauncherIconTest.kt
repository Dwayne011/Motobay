package uk.motobay.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws the launcher icon the way Android does (background + foreground through a
 * mask, and the monochrome layer on its own) to build/outputs/roborazzi/launcher-icon.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestMotoBayApplication::class)
class LauncherIconTest {
    @Test fun rendersAdaptiveIconLayers() {
        val context = ApplicationProvider.getApplicationContext<MotoBayApplication>()
        val icon = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null) as AdaptiveIconDrawable
        assertNotNull("monochrome layer", icon.monochrome)

        val size = 432 // 108dp at 4x
        val out = Bitmap.createBitmap(size * 3 + 64, size + 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out).apply { drawColor(Color.parseColor("#EEEEEE")) }

        // Full canvas (what the designer sees).
        icon.setBounds(16, 16, 16 + size, 16 + size)
        icon.draw(canvas)

        // Through a circular mask (what most launchers show).
        val circle = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(circle).let { c ->
            icon.setBounds(0, 0, size, size)
            icon.draw(c)
            val clear = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            val mask = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(mask).drawPath(Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }, Paint(Paint.ANTI_ALIAS_FLAG))
            c.drawBitmap(mask, 0f, 0f, clear)
        }
        canvas.drawBitmap(circle, 32f + size, 16f, null)

        // Monochrome layer tinted, as a themed icon would be.
        val mono = icon.monochrome!!.mutate()
        mono.setTint(Color.parseColor("#1D3A4F"))
        mono.setBounds(48 + size * 2, 16, 48 + size * 3, 16 + size)
        canvas.drawColor(Color.TRANSPARENT)
        Paint().apply { color = Color.parseColor("#D7E8F2") }.let { canvas.drawRect(48f + size * 2, 16f, 48f + size * 3, 16f + size, it) }
        mono.draw(canvas)

        val file = File("build/outputs/roborazzi/launcher-icon.png").apply { parentFile.mkdirs() }
        file.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }
}
