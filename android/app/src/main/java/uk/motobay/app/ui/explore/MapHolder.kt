package uk.motobay.app.ui.explore

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import uk.motobay.app.data.httpClient

/** What the Explore screen wants to hear from the map while it is on screen. */
interface MapCallbacks {
    fun onMoveStarted()
    fun onMoveCancelled()
    fun onIdle()
    fun onClick(point: LatLng): Boolean
    fun onLongClick(point: LatLng)
    fun onFailLoading()
}

/**
 * Owns the one MapView for the life of the activity, so leaving the Explore
 * screen (a tab, the bay page) pauses the map instead of destroying it.
 * Destroying the GL context on every navigation crashed the GPU driver on a
 * Pixel running Android 17 ("pure virtual function called" in the Vulkan
 * driver's semaphore thread as the context was torn down), and keeping the
 * map alive also means no style reload when the rider comes back.
 *
 * Listeners are registered on the map once and forwarded to whichever
 * composition currently owns [callbacks].
 */
class MapHolder(private val activity: Activity) {
    private var created: MapView? = null
    var map: MapLibreMap? = null
    var style: Style? = null
    /** Which theme [style] was loaded for. */
    var styleDark: Boolean? = null
    var callbacks: MapCallbacks? = null
    var listenersInstalled = false

    val mapView: MapView
        get() = created ?: createMapView(activity).also { created = it }

    /** The activity is going away: tear the map down with it. */
    fun destroy() {
        callbacks = null
        created?.onDestroy()
        created = null
        map = null
        style = null
    }

    companion object {
        fun createMapView(context: android.content.Context): MapView {
            MapLibre.getInstance(context.applicationContext)
            // Tiles and styles go through the app's client, so OpenFreeMap sees the same User-Agent as everything else.
            HttpRequestUtil.setOkHttpClient(httpClient)
            return MapView(context).apply { onCreate(null) }
        }
    }
}

/** The activity's map holder; null where there is none (tests, previews), in which case the map is local to the screen. */
val LocalMapHolder = staticCompositionLocalOf<MapHolder?> { null }
