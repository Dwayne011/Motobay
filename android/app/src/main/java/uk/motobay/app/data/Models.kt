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
    /** Set for bays added by riders (not from OpenStreetMap). */
    val addedBy: String? = null,
) {
    val isSample: Boolean get() = id.startsWith("sample/")
    val isRiderAdded: Boolean get() = id.startsWith("rider/")
}

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
