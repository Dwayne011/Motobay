package uk.motobay.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** JPEG of at most [maxSize]px on the longest side. Re-encoding also drops EXIF location data. */
fun downscaleToJpeg(context: Context, source: Uri, maxSize: Int = 1600): ByteArray {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSize) {
                val scale = maxSize.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val full = MediaStore.Images.Media.getBitmap(context.contentResolver, source)
        val scale = minOf(1f, maxSize.toFloat() / max(full.width, full.height))
        Bitmap.createScaledBitmap(full, (full.width * scale).roundToInt(), (full.height * scale).roundToInt(), true)
    }
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
}
