package uk.motobay.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import uk.motobay.app.data.Bay
import uk.motobay.app.data.LatLon
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance in metres. */
fun distanceMetres(a: LatLon, lat: Double, lon: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat - a.lat)
    val dLon = Math.toRadians(lon - a.lon)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(lat)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

fun distanceMetres(a: LatLon, bay: Bay) = distanceMetres(a, bay.lat, bay.lon)

/** UK-style distance: yards under a quarter mile, then miles. */
fun formatDistance(metres: Double): String {
    val miles = metres / 1609.344
    return when {
        miles < 0.25 -> "${((metres * 1.0936) / 10).roundToInt() * 10} yd"
        miles < 10 -> "%.1f mi".format(miles)
        else -> "${miles.roundToInt()} mi"
    }
}

/**
 * Starts turn-by-turn directions. Uses Google Maps navigation when it's
 * installed, otherwise lets the rider pick any maps app (Waze, etc.).
 */
fun openDirections(context: Context, bay: Bay) {
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${bay.lat},${bay.lon}"))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(nav)
    } catch (_: ActivityNotFoundException) {
        openInMapsApp(context, bay)
    }
}

/** Shows the bay in any installed maps app, via the standard geo: link. */
fun openInMapsApp(context: Context, bay: Bay) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${bay.lat},${bay.lon}?q=${bay.lat},${bay.lon}(${Uri.encode(bay.name)})"))
    try {
        context.startActivity(Intent.createChooser(geo, "Open with"))
    } catch (_: ActivityNotFoundException) {
        // No maps app installed; nothing to open.
    }
}

fun shareBay(context: Context, bay: Bay) {
    val text = "${bay.name}\nhttps://www.openstreetmap.org/${bay.id}"
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}
