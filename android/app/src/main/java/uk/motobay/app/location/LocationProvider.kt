package uk.motobay.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import uk.motobay.app.data.LatLon
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * The rider's position, using the platform LocationManager (no Google Play
 * services needed). Only requested when the rider taps "my location", and
 * never stored or sent anywhere.
 */
class LocationProvider(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private val _position = MutableStateFlow<LatLon?>(null)
    val position: StateFlow<LatLon?> = _position.asStateFlow()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Returns the current position, or null if permission is missing or location is off. */
    @SuppressLint("MissingPermission")
    suspend fun locate(): LatLon? {
        if (!hasPermission() || !LocationManagerCompat.isLocationEnabled(manager)) return null
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { it in manager.getProviders(true) } ?: return null
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val signal: android.os.CancellationSignal? = null
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor, Consumer { loc -> cont.resume(loc) })
        } ?: manager.getLastKnownLocation(provider)
        return location?.let { LatLon(it.latitude, it.longitude) }?.also { _position.value = it }
    }
}
