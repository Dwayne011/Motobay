package uk.motobay.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.motobay.app.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenStreetMap services: Overpass for bay data and Nominatim for place
 * search. The public servers are for light use only; point these URLs at a
 * self-hosted or paid instance before launch.
 */
class OsmApi(
    private val overpassUrl: String = "https://overpass-api.de/api/interpreter",
    private val nominatimUrl: String = "https://nominatim.openstreetmap.org/search",
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        // Nominatim and Overpass ask apps to identify themselves.
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "MotoBay/${BuildConfig.VERSION_NAME} (Android)")
                    .build(),
            )
        }
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Motorcycle parking inside a box: dedicated `amenity=motorcycle_parking`
     * plus car parks tagged `motorcycle=designated`.
     */
    suspend fun baysIn(b: BBox): List<Bay> {
        val box = "${b.south},${b.west},${b.north},${b.east}"
        return overpass(
            """
            [out:json][timeout:25];
            (
              nwr["amenity"="motorcycle_parking"]($box);
              nwr["amenity"="parking"]["motorcycle"="designated"]($box);
            );
            out center tags;
            """.trimIndent(),
        )
    }

    /** One bay by OSM id ("node/123"), for shared links. */
    suspend fun bay(id: String): Bay? {
        val (type, num) = id.split("/").takeIf { it.size == 2 } ?: return null
        if (type !in setOf("node", "way", "relation") || num.toLongOrNull() == null) return null
        return overpass("[out:json][timeout:25];$type($num);out center tags;").firstOrNull()
    }

    data class Place(val label: String, val bbox: BBox)

    /** Looks up a UK town, street or postcode. */
    suspend fun searchPlace(query: String): Place? = withContext(Dispatchers.IO) {
        val url = nominatimUrl.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("countrycodes", "gb")
            .addQueryParameter("limit", "1")
            .build()
        val body = execute(Request.Builder().url(url).header("Accept-Language", "en-GB").build())
        val first = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject ?: return@withContext null
        val bb = first["boundingbox"]!!.jsonArray.map { it.jsonPrimitive.content.toDouble() }
        Place(
            label = first["display_name"]?.jsonPrimitive?.content ?: query,
            bbox = BBox(south = bb[0], north = bb[1], west = bb[2], east = bb[3]),
        )
    }

    private suspend fun overpass(query: String): List<Bay> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(overpassUrl)
            .post(FormBody.Builder().add("data", query).build())
            .build()
        val elements = json.parseToJsonElement(execute(request)).jsonObject["elements"] as? JsonArray
        elements.orEmpty().mapNotNull { parseElement(it.jsonObject) }
    }

    private fun execute(request: Request): String =
        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Server error ${res.code}")
            res.body?.string() ?: throw IOException("Empty response")
        }

    companion object {
        fun parseElement(el: JsonObject): Bay? {
            val center = el["center"] as? JsonObject
            val lat = el["lat"]?.jsonPrimitive?.doubleOrNull ?: center?.get("lat")?.jsonPrimitive?.doubleOrNull ?: return null
            val lon = el["lon"]?.jsonPrimitive?.doubleOrNull ?: center?.get("lon")?.jsonPrimitive?.doubleOrNull ?: return null
            val tags = (el["tags"] as? JsonObject)
                ?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }
                .orEmpty()
            val street = tags["addr:street"]
            return Bay(
                id = "${el["type"]!!.jsonPrimitive.content}/${el["id"]!!.jsonPrimitive.content}",
                lat = lat,
                lon = lon,
                name = tags["name"] ?: street?.let { "$it motorcycle bay" } ?: "Motorcycle parking",
                fee = parseFee(tags),
                covered = tags["covered"]?.let { it == "yes" || it == "roof" || it == "partial" },
                capacity = (tags["capacity:motorcycle"] ?: tags["capacity"])?.toIntOrNull(),
                openingHours = tags["opening_hours"],
                maxStay = tags["maxstay"],
                operator = tags["operator"],
                street = street,
            )
        }

        private fun parseFee(tags: Map<String, String>): Fee {
            val fee = (tags["fee"] ?: tags["fee:motorcycle"]).orEmpty().lowercase()
            return when {
                fee == "no" -> Fee.Free
                fee.isNotEmpty() || tags.containsKey("charge") -> Fee.Paid
                else -> Fee.Unknown
            }
        }
    }
}
