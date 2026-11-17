package uk.motobay.app.data

import java.io.File
import java.time.YearMonth
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

/**
 * Police-recorded vehicle crime near the bays, built weekly from data.police.uk by
 * scripts/fetch_vehicle_crime.py. The police place each crime at an anonymised point
 * near where it happened, so these are counts nearby, never a safety rating.
 */
@Serializable
data class VehicleCrimeData(
    /** The months covered, newest first, e.g. ["2026-09", "2026-08", "2026-07"]. */
    val months: List<String> = emptyList(),
    val radiusMetres: Int = 200,
    /** The median bay's count, for comparison. */
    val typical: Int = 0,
    /** Crimes within [radiusMetres] of each bay, by bay id. */
    val bays: Map<String, Int> = emptyMap(),
    /** Bays the police publish nothing for (Scotland). */
    val noData: List<String> = emptyList(),
    /** Reported locations for the heat layer: [lat, lon, count]. */
    val points: List<List<Double>> = emptyList(),
    val attribution: String = "",
) {
    /** What the bay page says about one bay. */
    fun forBay(bayId: String): BayCrime? = when {
        bayId in noData -> BayCrime.NoData
        else -> bays[bayId]?.let { BayCrime.Count(it, typical, radiusMetres, period()) }
    }

    /** "Jul–Sep 2026", or "Nov 2026 – Jan 2027" across a new year. */
    fun period(): String {
        val ms = months.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.sorted()
        if (ms.isEmpty()) return ""
        // Fixed names: Android's own UK abbreviations vary by version ("Sep" or "Sept").
        fun name(m: YearMonth) = MONTHS[m.monthValue - 1]
        val first = ms.first()
        val last = ms.last()
        return when {
            first == last -> "${name(first)} ${first.year}"
            first.year == last.year -> "${name(first)}\u2013${name(last)} ${last.year}"
            else -> "${name(first)} ${first.year} \u2013 ${name(last)} ${last.year}"
        }
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

sealed interface BayCrime {
    data class Count(val count: Int, val typical: Int, val radiusMetres: Int, val period: String) : BayCrime
    /** Police Scotland doesn't publish street-level crime. */
    data object NoData : BayCrime
}

/** Downloads data/vehicle-crime.json when first needed, and keeps a copy on the phone. */
class VehicleCrimeRepository(
    dir: File,
    private val url: String,
    private val client: OkHttpClient = httpClient,
) {
    private val _data = MutableStateFlow<VehicleCrimeData?>(null)
    val data: StateFlow<VehicleCrimeData?> = _data.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile = File(dir, "vehicle-crime.json")
    private val etagFile = File(dir, "vehicle-crime.etag")
    private val mutex = Mutex()
    private var checked = false

    /** Tests only: a file to use instead of the download. */
    @androidx.annotation.VisibleForTesting
    var testDataset: String? = null

    /** Shows the saved copy, then (once per launch) checks for a newer one. Failures leave what's there. */
    suspend fun load() = mutex.withLock {
        testDataset?.let { _data.value = json.decodeFromString(VehicleCrimeData.serializer(), it); return@withLock }
        if (checked) return@withLock
        withContext(Dispatchers.IO) {
            if (_data.value == null && cacheFile.exists()) runCatching { publish(cacheFile.readText()) }
            runCatching {
                val request = Request.Builder().url(url).apply {
                    if (cacheFile.exists() && etagFile.exists()) header("If-None-Match", etagFile.readText())
                }.build()
                client.newCall(request).execute().use { res ->
                    if (res.code == 304) return@use
                    if (!res.isSuccessful) throw HttpException(res.code)
                    val body = res.body?.string() ?: return@use
                    publish(body)
                    cacheFile.writeText(body)
                    res.header("ETag")?.let { etagFile.writeText(it) }
                }
            }.onSuccess { checked = true }
        }
    }

    private fun publish(body: String) {
        _data.value = json.decodeFromString(VehicleCrimeData.serializer(), body)
    }
}
