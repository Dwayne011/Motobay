package uk.motobay.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** The bay data file built weekly by .github/workflows/update-bay-data.yml. */
@Serializable
data class BayDataset(val generated: String = "", val count: Int = 0, val bays: List<Bay> = emptyList())

sealed interface BayLoadState {
    data object Loading : BayLoadState
    data object Ready : BayLoadState
    data class Failed(val message: String) : BayLoadState
}

/**
 * Every UK motorcycle bay, from one data file that's downloaded once and kept
 * on the device. Later launches show the saved copy straight away and only
 * download again when the file has changed.
 */
class BayRepository(
    private val dir: File,
    private val url: String,
    private val useSampleData: Boolean,
    private val client: OkHttpClient = httpClient,
) {
    private val _bays = MutableStateFlow<Map<String, Bay>>(emptyMap())
    val bays: StateFlow<Map<String, Bay>> = _bays.asStateFlow()

    private val _state = MutableStateFlow<BayLoadState>(BayLoadState.Loading)
    val state: StateFlow<BayLoadState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile = File(dir, "uk-bays.json")
    private val etagFile = File(dir, "uk-bays.etag")
    private val mutex = Mutex()

    /** Shows the saved copy, then checks for a newer file. Safe to call again to retry. */
    suspend fun load() = mutex.withLock {
        if (useSampleData) {
            _bays.value = SampleBays.all.associateBy { it.id }
            _state.value = BayLoadState.Ready
            return@withLock
        }
        withContext(Dispatchers.IO) {
            if (_bays.value.isEmpty() && cacheFile.exists()) {
                runCatching { publish(cacheFile.readText()) }
            }
            if (_bays.value.isEmpty()) _state.value = BayLoadState.Loading
            runCatching { download() }
                .onSuccess { _state.value = BayLoadState.Ready }
                .onFailure { e ->
                    // A saved copy is still usable, so only report failure when there's nothing to show.
                    _state.value = if (_bays.value.isNotEmpty()) BayLoadState.Ready
                    else BayLoadState.Failed("Couldn't load bays: ${e.friendlyMessage()}")
                }
        }
    }

    suspend fun bay(id: String): Bay? {
        if (_bays.value.isEmpty()) load()
        return _bays.value[id]
    }

    private fun download() {
        val request = Request.Builder().url(url).apply {
            if (cacheFile.exists() && etagFile.exists()) header("If-None-Match", etagFile.readText())
        }.build()
        client.newCall(request).execute().use { res ->
            if (res.code == 304) return
            if (!res.isSuccessful) throw HttpException(res.code)
            val body = res.body?.string() ?: return
            publish(body)
            cacheFile.writeText(body)
            res.header("ETag")?.let { etagFile.writeText(it) }
        }
    }

    private fun publish(body: String) {
        val dataset = json.decodeFromString(BayDataset.serializer(), body)
        _bays.value = dataset.bays.associateBy { it.id }
    }
}
