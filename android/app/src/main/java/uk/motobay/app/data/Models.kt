package uk.motobay.app.data

import kotlinx.serialization.Serializable

enum class Fee { Free, Paid, Unknown }

/** A motorcycle parking location, normalised from OpenStreetMap tags. */
@Serializable
data class Bay(
    /** OSM element id, for example "node/123456". */
    val id: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val fee: Fee,
    val covered: Boolean?,
    val capacity: Int?,
    val openingHours: String?,
    val maxStay: String?,
    val operator: String?,
    val street: String?,
) {
    val isSample: Boolean get() = id.startsWith("sample/")
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
