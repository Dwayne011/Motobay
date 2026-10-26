package uk.motobay.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Fee {
    @SerialName("free") Free,
    @SerialName("paid") Paid,
    @SerialName("unknown") Unknown,
}

/** A motorcycle parking location, normalised from OpenStreetMap tags. */
@Serializable
data class Bay(
    /** OSM element id, for example "node/123456". */
    val id: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val fee: Fee = Fee.Unknown,
    // Optional fields are left out of the data file when empty.
    val covered: Boolean? = null,
    val capacity: Int? = null,
    val openingHours: String? = null,
    val maxStay: String? = null,
    val operator: String? = null,
    val street: String? = null,
    /** Neighbourhood or town from reverse geocoding, e.g. "Spitalfields". */
    val area: String? = null,
    /** The town when it differs from [area], e.g. "London". */
    val city: String? = null,
    /** Outward postcode, e.g. "E1". */
    val postcode: String? = null,
    /** Set for bays added by riders (not from OpenStreetMap). */
    val addedBy: String? = null,
) {
    val isSample: Boolean get() = id.startsWith("sample/")
    val isRiderAdded: Boolean get() = id.startsWith("rider/")

    /** True when the mapper gave the bay no real name, just what it is. */
    val hasGenericName: Boolean get() = name.trim().lowercase() in GENERIC_NAMES

    /** What to call the bay: its name, or failing that its street. */
    val title: String get() = if (hasGenericName) street ?: name else name

    /**
     * Where it is, for the line under the title: street (unless it is the title),
     * then area, town and outward postcode, e.g. "Spitalfields, London E1".
     */
    val locality: String?
        get() {
            val place = listOfNotNull(area, city?.takeIf { it != area }).joinToString(", ")
            val placeWithPostcode = listOfNotNull(place.ifEmpty { null }, postcode).joinToString(" ")
            val streetPart = street?.takeIf { it != title }
            return listOfNotNull(streetPart, placeWithPostcode.ifEmpty { null }).joinToString(", ").ifEmpty { null }
        }
}

/** Names OpenStreetMap mappers give bays that say what they are, not where. */
private val GENERIC_NAMES = setOf(
    "motorcycle parking", "motorcycle bay", "motorcycle bays", "solo m/c bay", "solo motorcycle bay", "motorbike parking",
)

data class BBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    operator fun contains(b: Bay) = b.lat in south..north && b.lon in west..east
}

data class LatLon(val lat: Double, val lon: Double)

@Serializable
enum class ModerationStatus { Pending, Approved, Rejected }

@Serializable
data class BayPhoto(
    val id: String,
    val bayId: String,
    val bayName: String,
    /** Local file path in the mock backend. A real backend would return a URL. */
    val path: String,
    val caption: String,
    val author: String,
    val createdAt: Long,
    val status: ModerationStatus,
)

@Serializable
data class BayComment(
    val id: String,
    val bayId: String,
    val bayName: String,
    val text: String,
    val author: String,
    val createdAt: Long,
    val status: ModerationStatus,
)

/**
 * A rider's new bay, or a correction to an existing one ([editOf] is the bay's
 * id). Held for review like photos and comments; once approved it shows on the map.
 */
@Serializable
data class BaySubmission(
    val id: String,
    val editOf: String? = null,
    val lat: Double,
    val lon: Double,
    val name: String,
    val fee: Fee,
    val capacity: Int?,
    val covered: Boolean?,
    val notes: String,
    val author: String,
    val createdAt: Long,
    val status: ModerationStatus,
)
