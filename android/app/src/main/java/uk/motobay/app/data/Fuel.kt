package uk.motobay.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.motobay.app.util.distanceMetres
import java.io.File
import java.util.zip.GZIPInputStream

/** The fuels a motorcycle uses, as Fuel Finder names them. */
enum class FuelGrade(val label: String, val short: String) {
    E10("Unleaded (E10)", "E10"),
    E5("Super unleaded (E5)", "E5"),
}

/** One forecourt and its current prices in pence per litre (null when it doesn't sell that grade). */
data class FuelStation(
    val id: String,
    val name: String,
    val brand: String?,
    val lat: Double,
    val lon: Double,
    val postcode: String?,
    val e10: Double?,
    val e5: Double?,
    val updated: String?,
    val motorway: Boolean,
) {
    fun price(grade: FuelGrade): Double? = when (grade) {
        FuelGrade.E10 -> e10
        FuelGrade.E5 -> e5
    }

    /** "Shell Euston Road" or the brand and name, without saying the brand twice. */
    val title: String
        get() = if (brand == null || name.contains(brand, ignoreCase = true)) name else "$brand $name"
}

data class FuelData(val generated: String, val stations: List<FuelStation>)

/** Parses fuel/prices.json (see functions/fuel.js): rows of id, name, brand, lat, lon, postcode, e10, e5, b7, updated, motorway. */
fun parseFuel(text: String): FuelData {
    val root = Json.parseToJsonElement(text).jsonObject
    val stations = root["stations"]!!.jsonArray.mapNotNull { row ->
        val r = row as? JsonArray ?: return@mapNotNull null
        fun s(i: Int) = r.getOrNull(i)?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull
        fun d(i: Int) = r.getOrNull(i)?.takeIf { it !is JsonNull }?.jsonPrimitive?.doubleOrNull
        val lat = d(3) ?: return@mapNotNull null
        val lon = d(4) ?: return@mapNotNull null
        FuelStation(
            id = s(0) ?: return@mapNotNull null,
            name = s(1) ?: "Petrol station",
            brand = s(2),
            lat = lat, lon = lon,
            postcode = s(5),
            e10 = d(6), e5 = d(7),
            updated = s(9),
            motorway = r.getOrNull(10)?.takeIf { it !is JsonNull }?.jsonPrimitive?.booleanOrNull == true,
        )
    }
    return FuelData(root["generated"]?.jsonPrimitive?.contentOrNull ?: "", stations)
}

/** A station with its distance from where the rider is looking. */
data class FuelNearby(val station: FuelStation, val metres: Double, val price: Double)

/**
 * The cheapest forecourts selling [grade] within [radiusMetres] of [at], cheapest first and
 * nearest on a tie. Every brand is treated the same: only price and distance decide.
 */
fun cheapestNear(stations: List<FuelStation>, at: LatLon, grade: FuelGrade, radiusMetres: Double = 8_000.0, limit: Int = 3): List<FuelNearby> =
    stations.asSequence()
        .mapNotNull { s -> s.price(grade)?.let { p -> FuelNearby(s, distanceMetres(at, s.lat, s.lon), p) } }
        .filter { it.metres <= radiusMetres }
        .sortedWith(compareBy({ it.price }, { it.metres }))
        .take(limit)
        .toList()

/** "139.9p" */
fun formatPence(p: Double): String = "%.1fp".format(java.util.Locale.UK, p)

/**
 * Downloads fuel/prices.json.gz from Cloud Storage (kept up to date every 30 minutes by the
 * refreshFuelPrices Cloud Function) when the fuel layer is first shown, and keeps a copy.
 */
open class FuelRepository(private val dir: File) {
    private val _data = MutableStateFlow<FuelData?>(null)
    val data: StateFlow<FuelData?> = _data.asStateFlow()
    private val cacheFile = File(dir, "fuel-prices.json.gz")
    private val stampFile = File(dir, "fuel-prices.updated")
    private val mutex = Mutex()
    private var lastCheck = 0L

    /** Tests only: a file to use instead of the download. */
    @androidx.annotation.VisibleForTesting
    var testDataset: String? = null

    /** Shows the saved copy, then checks for newer prices at most every 10 minutes. Failures leave what's there. */
    suspend fun load() = mutex.withLock {
        testDataset?.let { _data.value = parseFuel(it); return@withLock }
        withContext(Dispatchers.IO) {
            if (_data.value == null && cacheFile.exists()) runCatching { publish(cacheFile.readBytes()) }
            if (System.currentTimeMillis() - lastCheck < CHECK_EVERY_MS) return@withContext
            runCatching {
                val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(PATH)
                val updated = ref.metadata.await().updatedTimeMillis
                if (!cacheFile.exists() || stampFile.takeIf { it.exists() }?.readText()?.toLongOrNull() != updated) {
                    val bytes = ref.getBytes(MAX_BYTES).await()
                    publish(bytes)
                    cacheFile.writeBytes(bytes)
                    stampFile.writeText(updated.toString())
                }
            }.onSuccess { lastCheck = System.currentTimeMillis() }
        }
    }

    private fun publish(gz: ByteArray) {
        _data.value = parseFuel(GZIPInputStream(gz.inputStream()).bufferedReader().use { it.readText() })
    }

    companion object {
        const val PATH = "fuel/prices.json.gz"
        private const val MAX_BYTES = 10L * 1024 * 1024
        private const val CHECK_EVERY_MS = 10 * 60 * 1000L
    }
}
