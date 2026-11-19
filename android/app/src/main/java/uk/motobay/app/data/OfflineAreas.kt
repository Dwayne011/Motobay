package uk.motobay.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** A map area saved on the phone for use without signal. */
data class OfflineArea(val id: Long, val name: String, val createdAt: Long, val bytes: Long, val complete: Boolean)

/** Map areas downloaded for offline use. The bays themselves are always kept on the phone. */
interface OfflineAreas {
    val areas: StateFlow<List<OfflineArea>>

    /** Download progress from 0 to 1 while a download runs, otherwise null. */
    val progress: StateFlow<Float?>

    fun refresh()

    /** Downloads the map for [box] in the given style. Throws if it fails. */
    suspend fun download(name: String, box: BBox, styleUrl: String)

    suspend fun delete(id: Long)
}

/** The base map's vector tiles go down to zoom 14; closer zooms are drawn from those. */
const val OFFLINE_MIN_ZOOM = 6.0
const val OFFLINE_MAX_ZOOM = 14.0

/** Bigger than about this many tiles (roughly 80 km across) is refused: zoom in and save a smaller area. */
const val OFFLINE_TILE_LIMIT = 3_000

/** How many map tiles [box] needs from [minZoom] to [maxZoom], for the size check. */
fun tileCount(box: BBox, minZoom: Int = OFFLINE_MIN_ZOOM.toInt(), maxZoom: Int = OFFLINE_MAX_ZOOM.toInt()): Long {
    fun x(lon: Double, z: Int) = floor((lon + 180.0) / 360.0 * (1 shl z)).toLong()
    fun y(lat: Double, z: Int): Long {
        val r = Math.toRadians(lat)
        return floor((1.0 - ln(tan(r) + 1.0 / kotlin.math.cos(r)) / PI) / 2.0 * (1 shl z)).toLong()
    }
    return (minZoom..maxZoom).sumOf { z -> (x(box.east, z) - x(box.west, z) + 1) * (y(box.south, z) - y(box.north, z) + 1) }
}

@Serializable
private data class AreaMeta(val name: String, val createdAt: Long)

class MapLibreOfflineAreas(private val context: Context) : OfflineAreas {
    private val _areas = MutableStateFlow<List<OfflineArea>>(emptyList())
    override val areas = _areas.asStateFlow()
    private val _progress = MutableStateFlow<Float?>(null)
    override val progress = _progress.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    private val manager by lazy {
        MapLibre.getInstance(context.applicationContext)
        OfflineManager.getInstance(context.applicationContext)
    }

    override fun refresh() {
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val regions = offlineRegions.orEmpty()
                val result = mutableListOf<OfflineArea>()
                if (regions.isEmpty()) _areas.value = emptyList()
                regions.forEach { region ->
                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            val meta = runCatching { json.decodeFromString(AreaMeta.serializer(), region.metadata.decodeToString()) }.getOrNull()
                            result += OfflineArea(
                                region.id, meta?.name ?: "Map area", meta?.createdAt ?: 0,
                                status?.completedResourceSize ?: 0, status?.isComplete == true,
                            )
                            if (result.size == regions.size) _areas.value = result.sortedByDescending { it.createdAt }
                        }

                        override fun onError(error: String?) {
                            result += OfflineArea(region.id, "Map area", 0, 0, false)
                            if (result.size == regions.size) _areas.value = result.sortedByDescending { it.createdAt }
                        }
                    })
                }
            }

            override fun onError(error: String) {}
        })
    }

    override suspend fun download(name: String, box: BBox, styleUrl: String) {
        val bounds = LatLngBounds.Builder().include(LatLng(box.south, box.west)).include(LatLng(box.north, box.east)).build()
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl, bounds, OFFLINE_MIN_ZOOM, OFFLINE_MAX_ZOOM, context.resources.displayMetrics.density,
        )
        val meta = json.encodeToString(AreaMeta.serializer(), AreaMeta(name, System.currentTimeMillis())).encodeToByteArray()
        val region = suspendCancellableCoroutine { cont ->
            manager.createOfflineRegion(definition, meta, object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) = cont.resume(offlineRegion)
                override fun onError(error: String) = cont.resumeWithException(IllegalStateException(error))
            })
        }
        _progress.value = 0f
        try {
            suspendCancellableCoroutine { cont ->
                region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        val required = status.requiredResourceCount.coerceAtLeast(1)
                        _progress.value = (status.completedResourceCount.toFloat() / required).coerceIn(0f, 1f)
                        if (status.isComplete) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }

                    override fun onError(error: OfflineRegionError) {
                        // Single tiles can fail and be retried by MapLibre; only stop on a hard error.
                        if (error.reason == OfflineRegionError.REASON_NOT_FOUND) return
                        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        if (cont.isActive) cont.resumeWithException(IllegalStateException(error.message))
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        if (cont.isActive) cont.resumeWithException(IllegalStateException("Too many map tiles"))
                    }
                })
                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                cont.invokeOnCancellation { region.setDownloadState(OfflineRegion.STATE_INACTIVE) }
            }
        } finally {
            _progress.value = null
            refresh()
        }
    }

    override suspend fun delete(id: Long) {
        val region = suspendCancellableCoroutine<OfflineRegion?> { cont ->
            manager.getOfflineRegion(id, object : OfflineManager.GetOfflineRegionCallback {
                override fun onRegion(offlineRegion: OfflineRegion) = cont.resume(offlineRegion)
                override fun onRegionNotFound() = cont.resume(null)
                override fun onError(error: String) = cont.resumeWithException(IllegalStateException(error))
            })
        } ?: return refresh()
        suspendCancellableCoroutine { cont ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() = cont.resume(Unit)
                override fun onError(error: String) = cont.resumeWithException(IllegalStateException(error))
            })
        }
        refresh()
    }
}
