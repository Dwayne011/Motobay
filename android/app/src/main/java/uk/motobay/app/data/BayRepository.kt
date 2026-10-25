package uk.motobay.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.floor

/**
 * Loads bays on demand for whatever part of the UK is on screen. Results are
 * cached in 0.05° tiles (roughly 5 km) so panning back doesn't refetch.
 */
class BayRepository(private val api: OsmApi, private val useSampleData: Boolean) {
    private val _bays = MutableStateFlow<Map<String, Bay>>(emptyMap())
    val bays: StateFlow<Map<String, Bay>> = _bays.asStateFlow()

    private val loadedTiles = mutableSetOf<Pair<Int, Int>>()
    private val mutex = Mutex()

    /** Loads every bay in the box. Throws if the server can't be reached. */
    suspend fun ensureLoaded(box: BBox) {
        val missing = mutex.withLock {
            tilesFor(box).filter { it !in loadedTiles }.also { loadedTiles.addAll(it) }
        }
        if (missing.isEmpty()) return
        val area = bboxOf(missing)
        val result = runCatching {
            if (useSampleData) SampleBays.all.filter { it in area } else api.baysIn(area)
        }
        result.onSuccess { list -> _bays.update { current -> current + list.associateBy { it.id } } }
        result.onFailure {
            mutex.withLock { loadedTiles.removeAll(missing.toSet()) }
            throw it
        }
    }

    /** One bay from cache or the server, for shared links and saved bays. */
    suspend fun bay(id: String): Bay? {
        _bays.value[id]?.let { return it }
        val bay = if (useSampleData) SampleBays.all.firstOrNull { it.id == id } else api.bay(id)
        bay?.let { b -> _bays.update { it + (b.id to b) } }
        return bay
    }

    private fun tilesFor(b: BBox): List<Pair<Int, Int>> {
        val ys = floor(b.south / TILE).toInt()..floor(b.north / TILE).toInt()
        val xs = floor(b.west / TILE).toInt()..floor(b.east / TILE).toInt()
        return ys.flatMap { y -> xs.map { x -> y to x } }
    }

    private fun bboxOf(tiles: List<Pair<Int, Int>>) = BBox(
        south = tiles.minOf { it.first } * TILE,
        north = (tiles.maxOf { it.first } + 1) * TILE,
        west = tiles.minOf { it.second } * TILE,
        east = (tiles.maxOf { it.second } + 1) * TILE,
    )

    private companion object {
        const val TILE = 0.05
    }
}
