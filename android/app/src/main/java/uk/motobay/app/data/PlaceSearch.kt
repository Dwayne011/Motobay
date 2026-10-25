package uk.motobay.app.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/** One row in the search suggestions list. */
data class PlaceSuggestion(
    val title: String,
    val subtitle: String,
    val kind: Kind,
    /** Where to go. Null for postcodes until they're resolved. */
    val point: LatLon?,
    val bbox: BBox?,
) {
    enum class Kind { Town, Street, Postcode, Place }
}

/**
 * Search-as-you-type for UK places:
 * - Photon (OpenStreetMap, built for autocomplete) for towns, streets and places
 * - postcodes.io (Royal Mail / ONS open data) for postcodes
 *
 * Both are free public services with fair-use limits. Before launch, consider
 * self-hosting Photon or switching to a paid provider.
 */
class PlaceSearch(
    /** Performs a request and returns the body. Tests swap in recorded responses. */
    private val fetch: (Request) -> String = { httpClient.fetch(it) },
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Suggestions for what the rider has typed so far, best first. */
    suspend fun suggest(query: String, near: LatLon?): List<PlaceSuggestion> = coroutineScope {
        val q = query.trim()
        if (q.length < 2) return@coroutineScope emptyList()
        val postcodes: Deferred<List<PlaceSuggestion>>? = if (POSTCODE_START.matches(q)) async { runCatching { postcodes(q) }.getOrDefault(emptyList()) } else null
        val places = async { places(q, near) }
        (postcodes?.await().orEmpty() + places.await()).distinctBy { it.title to it.subtitle }.take(8)
    }

    /** Looks up the coordinates of a full postcode. */
    suspend fun resolvePostcode(postcode: String): LatLon? = withContext(Dispatchers.IO) {
        val url = "https://api.postcodes.io/postcodes".toHttpUrl().newBuilder().addPathSegment(postcode).build()
        val result = json.parseToJsonElement(fetch(Request.Builder().url(url).build())).jsonObject["result"]?.jsonObject
            ?: return@withContext null
        val lat = result["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@withContext null
        val lon = result["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@withContext null
        LatLon(lat, lon)
    }

    private suspend fun places(q: String, near: LatLon?): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val url = "https://photon.komoot.io/api/".toHttpUrl().newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("limit", "10")
            .addQueryParameter("lang", "en")
            .addQueryParameter("bbox", UK_BBOX)
            .apply {
                if (near != null) {
                    addQueryParameter("lat", near.lat.toString())
                    addQueryParameter("lon", near.lon.toString())
                }
            }
            .build()
        parsePhoton(fetch(Request.Builder().url(url).build()))
    }

    private suspend fun postcodes(q: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val url = "https://api.postcodes.io/postcodes".toHttpUrl().newBuilder()
            .addPathSegment(q.replace(" ", ""))
            .addPathSegment("autocomplete")
            .addQueryParameter("limit", "4")
            .build()
        val result = json.parseToJsonElement(fetch(Request.Builder().url(url).build())).jsonObject["result"]
        val list = runCatching { result!!.jsonArray }.getOrNull().orEmpty()
        list.map { PlaceSuggestion(it.jsonPrimitive.content, "Postcode", PlaceSuggestion.Kind.Postcode, null, null) }
    }

    /** Turns a Photon GeoJSON response into suggestions, keeping UK results only. */
    fun parsePhoton(body: String): List<PlaceSuggestion> =
        json.parseToJsonElement(body).jsonObject["features"]!!.jsonArray.mapNotNull { f ->
            val props = f.jsonObject["properties"]?.jsonObject ?: return@mapNotNull null
            if (props.str("countrycode") != "GB") return@mapNotNull null
            val coords = f.jsonObject["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: return@mapNotNull null
            val point = LatLon(coords[1].jsonPrimitive.content.toDouble(), coords[0].jsonPrimitive.content.toDouble())
            val type = props.str("type")
            // Named places use their name; plain addresses use "number street".
            val title = props.str("name")
                ?: listOfNotNull(props.str("housenumber"), props.str("street")).joinToString(" ").ifEmpty { null }
                ?: return@mapNotNull null
            val kind = when {
                FULL_POSTCODE.matches(title) -> PlaceSuggestion.Kind.Postcode
                type in setOf("city", "town", "village", "district", "locality", "county", "state") -> PlaceSuggestion.Kind.Town
                type == "street" -> PlaceSuggestion.Kind.Street
                else -> PlaceSuggestion.Kind.Place
            }
            val subtitle = listOfNotNull(
                props.str("street").takeIf { kind == PlaceSuggestion.Kind.Place && it != title },
                props.str("city") ?: props.str("district"),
                props.str("county"),
                props.str("postcode"),
            ).filter { it != title }.distinct().joinToString(", ")
            // Photon's extent is [minLon, maxLat, maxLon, minLat].
            val bbox = props["extent"]?.jsonArray?.map { it.jsonPrimitive.content.toDouble() }?.let {
                BBox(south = it[3], west = it[0], north = it[1], east = it[2])
            }
            PlaceSuggestion(title, subtitle, kind, point, bbox)
        }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private companion object {
        /** Great Britain and Northern Ireland, as minLon,minLat,maxLon,maxLat. */
        const val UK_BBOX = "-8.7,49.8,1.9,60.9"

        /** Looks like the start of a UK postcode, e.g. "SW1", "M1 1", "EH12 9". */
        val POSTCODE_START = Regex("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?(\\s*\\d[A-Za-z]{0,2})?$")

        val FULL_POSTCODE = Regex("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?\\s*\\d[A-Za-z]{2}$")
    }
}
