package uk.motobay.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
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
    /**
     * Shows the Review queue. A local demo switch only. In production,
     * moderator access must come from the backend (a role on your account).
     */
    val moderator: Boolean = false,
    /** Last map position, so the app reopens where the rider left it. */
    val lastMap: CameraSnapshot? = null,
    /** Places picked from search, newest first. */
    val recentPlaces: List<RecentPlace> = emptyList(),
) {
    companion object {
        const val DEFAULT_SOURCE_COLOR = 0xFF0B6E99
        val PRESETS = listOf(0xFF0B6E99, 0xFF6750A4, 0xFFB3261E, 0xFF386A20, 0xFF8B5000, 0xFF006A6A)
    }
}

@Serializable
data class CameraSnapshot(val lat: Double, val lon: Double, val zoom: Double)

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

    val settings = settingsStore.state
    /** Saved bays, stored as snapshots so the Saved list works offline. */
    val favourites = favouritesStore.state

    fun updateSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    fun addRecentPlace(place: RecentPlace) = settingsStore.update { st ->
        val rest = st.recentPlaces.filterNot { it.title == place.title && it.subtitle == place.subtitle }
        st.copy(recentPlaces = (listOf(place) + rest).take(MAX_RECENTS))
    }

    fun clearRecentPlaces() = settingsStore.update { it.copy(recentPlaces = emptyList()) }

    fun toggleFavourite(bay: Bay) = favouritesStore.update { f ->
        if (bay.id in f) f - bay.id else f + (bay.id to bay)
    }
}

private const val MAX_RECENTS = 8
