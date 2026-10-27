package uk.motobay.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

// Firestore collections and field names. firestore.rules validates exactly these.
internal const val PHOTOS = "photos"
internal const val COMMENTS = "comments"
internal const val BAYS = "bays"

private fun Map<String, Any?>.str(key: String) = this[key] as? String
private fun Map<String, Any?>.num(key: String) = (this[key] as? Number)
private fun Map<String, Any?>.status() = ModerationStatus.entries.firstOrNull { it.name == this["status"] }
private fun Map<String, Any?>.millis() = (this["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()

internal fun photoFrom(id: String, m: Map<String, Any?>): BayPhoto? {
    return BayPhoto(
        id = id,
        bayId = m.str("bayId") ?: return null,
        bayName = m.str("bayName").orEmpty(),
        url = m.str("url") ?: return null,
        caption = m.str("caption").orEmpty(),
        author = m.str("author").orEmpty(),
        createdAt = m.millis(),
        status = m.status() ?: return null,
        authorUid = m.str("authorUid").orEmpty(),
        storagePath = m.str("storagePath").orEmpty(),
        reason = m.str("reason")?.takeIf { it.isNotBlank() },
    )
}

internal fun commentFrom(id: String, m: Map<String, Any?>): BayComment? {
    return BayComment(
        id = id,
        bayId = m.str("bayId") ?: return null,
        bayName = m.str("bayName").orEmpty(),
        text = m.str("text") ?: return null,
        author = m.str("author").orEmpty(),
        createdAt = m.millis(),
        status = m.status() ?: return null,
        authorUid = m.str("authorUid").orEmpty(),
        reason = m.str("reason")?.takeIf { it.isNotBlank() },
    )
}

internal fun baySubmissionFrom(id: String, m: Map<String, Any?>): BaySubmission? {
    return BaySubmission(
        id = id,
        editOf = m.str("editOf"),
        lat = m.num("lat")?.toDouble() ?: return null,
        lon = m.num("lon")?.toDouble() ?: return null,
        name = m.str("name").orEmpty(),
        fee = Fee.entries.firstOrNull { it.name == m["fee"] } ?: Fee.Unknown,
        capacity = m.num("capacity")?.toInt(),
        covered = m["covered"] as? Boolean,
        notes = m.str("notes").orEmpty(),
        author = m.str("author").orEmpty(),
        createdAt = m.millis(),
        status = m.status() ?: return null,
        authorUid = m.str("authorUid").orEmpty(),
        reason = m.str("reason")?.takeIf { it.isNotBlank() },
    )
}

internal fun photoToMap(bay: Bay, url: String, storagePath: String, caption: String, author: String, uid: String) = mapOf(
    "bayId" to bay.id,
    "bayName" to bay.name.take(200),
    "url" to url,
    "storagePath" to storagePath,
    "caption" to caption,
    "author" to author,
    "authorUid" to uid,
    "createdAt" to FieldValue.serverTimestamp(),
    "status" to ModerationStatus.Pending.name,
)

internal fun commentToMap(bay: Bay, text: String, author: String, uid: String) = mapOf(
    "bayId" to bay.id,
    "bayName" to bay.name.take(200),
    "text" to text,
    "author" to author,
    "authorUid" to uid,
    "createdAt" to FieldValue.serverTimestamp(),
    "status" to ModerationStatus.Pending.name,
)

/** What the admin writes when deciding: the new status, plus a reason when rejecting. */
internal fun decisionToMap(status: ModerationStatus, reason: String?): Map<String, Any> = mapOf(
    "status" to status.name,
    "reason" to (reason?.trim()?.takeIf { it.isNotEmpty() && status == ModerationStatus.Rejected }?.take(REASON_MAX) ?: FieldValue.delete()),
)

internal const val REASON_MAX = 300

/** Optional fields are left out when empty, as the rules expect. */
internal fun baySubmissionToMap(s: BaySubmission, uid: String, status: ModerationStatus): Map<String, Any> = buildMap {
    s.editOf?.let { put("editOf", it) }
    put("lat", s.lat)
    put("lon", s.lon)
    put("name", s.name.take(100))
    put("fee", s.fee.name)
    s.capacity?.let { put("capacity", it) }
    s.covered?.let { put("covered", it) }
    put("notes", s.notes.take(500))
    put("author", s.author)
    put("authorUid", uid)
    put("createdAt", FieldValue.serverTimestamp())
    put("status", status.name)
}
