package uk.motobay.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import java.io.File

enum class ThemeMode { System, Light, Dark }

@Serializable
data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Seed for every M3 tonal palette, as ARGB. */
    val sourceColor: Long = DEFAULT_SOURCE_COLOR,
    /** Android 12+: take colours from the wallpaper instead of the seed. */
    val wallpaperColors: Boolean = false,
    val displayName: String = "",
    /** The first-launch "guidance only, no PCN liability" notice has been read. */
    val disclaimerAccepted: Boolean = false,
    /** The first-launch request for location has been made; it is never repeated unprompted. */
    val locationAsked: Boolean = false,
    /** Last map position, shown at launch until (or instead of) the rider's own position. */
    val lastMap: CameraSnapshot? = null,
    /** Places picked from search, newest first. */
    val recentPlaces: List<RecentPlace> = emptyList(),
    /** Riders whose photos and comments this device hides. Kept locally; the admin still sees everything. */
    val blocked: List<BlockedUser> = emptyList(),
    /** The rider's last known position, for the home-screen widget. Only on the phone. */
    val lastFix: LastFix? = null,
) {
    companion object {
        const val DEFAULT_SOURCE_COLOR = 0xFF0B6E99
        val PRESETS = listOf(0xFF0B6E99, 0xFF6750A4, 0xFFB3261E, 0xFF386A20, 0xFF8B5000, 0xFF006A6A)
    }
}

@Serializable
data class CameraSnapshot(
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    /** The visible area's edges, for "download this area" (null in older saved settings). */
    val south: Double? = null,
    val west: Double? = null,
    val north: Double? = null,
    val east: Double? = null,
) {
    val box: BBox? get() = if (south != null && west != null && north != null && east != null) BBox(south, west, north, east) else null
}

@Serializable
data class LastFix(val lat: Double, val lon: Double)

@Serializable
data class BlockedUser(val uid: String, val name: String)

@Serializable
data class RecentPlace(val title: String, val subtitle: String, val lat: Double, val lon: Double)

/** Saved bays and app settings, stored on the device. */
class UserRepository(dir: File, scope: CoroutineScope) {
    private val settingsStore = JsonFileStore(File(dir, "settings.json"), Settings.serializer(), Settings(), scope)
    private val favouritesStore = JsonFileStore(
        File(dir, "favourites.json"),
        MapSerializer(String.serializer(), Bay.serializer()),
        emptyMap(),
        scope,
    )

    private val parkedStore = JsonFileStore(File(dir, "parked.json"), Parked.serializer().nullable, null, scope)

    val settings = settingsStore.state

    /** Where the rider says they parked, or null. Only ever on the phone. */
    val parked = parkedStore.state

    fun setParked(parked: Parked?) = parkedStore.update { parked }
    /** Saved bays, stored as snapshots so the Saved list works offline. */
    val favourites = favouritesStore.state

    fun updateSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    fun addRecentPlace(place: RecentPlace) = settingsStore.update { st ->
        val rest = st.recentPlaces.filterNot { it.title == place.title && it.subtitle == place.subtitle }
        st.copy(recentPlaces = (listOf(place) + rest).take(MAX_RECENTS))
    }

    fun clearRecentPlaces() = settingsStore.update { it.copy(recentPlaces = emptyList()) }

    fun block(uid: String, name: String) = settingsStore.update { st ->
        if (st.blocked.any { it.uid == uid }) st else st.copy(blocked = st.blocked + BlockedUser(uid, name))
    }

    fun unblock(uid: String) = settingsStore.update { st -> st.copy(blocked = st.blocked.filterNot { it.uid == uid }) }

    fun toggleFavourite(bay: Bay) = favouritesStore.update { f ->
        if (bay.id in f) f - bay.id else f + (bay.id to bay)
    }
}

private const val MAX_RECENTS = 8
