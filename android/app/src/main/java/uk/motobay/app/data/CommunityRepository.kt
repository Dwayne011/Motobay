package uk.motobay.app.data

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class CommunityData(
    val photos: List<BayPhoto> = emptyList(),
    val comments: List<BayComment> = emptyList(),
    val bays: List<BaySubmission> = emptyList(),
    val ratings: List<BayRating> = emptyList(),
)

/**
 * One rider's 1 to 5 star rating of a bay. A rider has at most one per bay,
 * and it is public straight away: there is no text to moderate.
 */
@Serializable
data class BayRating(val bayId: String, val stars: Int, val authorUid: String, val updatedAt: Long)

/** A bay's average star rating and how many riders gave one. */
data class RatingSummary(val average: Double, val count: Int)

fun CommunityData.ratingSummary(bayId: String): RatingSummary? {
    val stars = ratings.filter { it.bayId == bayId }.map { it.stars }
    return if (stars.isEmpty()) null else RatingSummary(stars.average(), stars.size)
}

fun CommunityData.ratingBy(uid: String?, bayId: String): BayRating? =
    uid?.let { u -> ratings.firstOrNull { it.bayId == bayId && it.authorUid == u } }

/** The document id of a rider's rating of a bay; firestore.rules checks it, which is what makes it one per rider per bay. */
fun ratingId(uid: String, bayId: String) = uid + "_" + bayId.replace('/', '_')

/**
 * Photos, comments, new bays and bay corrections that riders submit.
 * Everything starts as Pending and is only shown publicly after the admin
 * approves it. [FirestoreCommunityRepository] is the real implementation.
 *
 * [data] holds what the signed-in rider is allowed to see: approved items,
 * their own pending and rejected items, and (for the admin) everything.
 * Submitting needs a signed-in rider; the functions throw if there isn't one
 * or the server refuses.
 */
interface CommunityRepository {
    val data: StateFlow<CommunityData>

    suspend fun submitPhoto(bay: Bay, source: Uri, caption: String, author: String)

    suspend fun submitComment(bay: Bay, text: String, author: String)

    /** A new bay, or a correction when [BaySubmission.editOf] is set. Held for review like everything else. */
    suspend fun submitBay(submission: BaySubmission)

    /** Admin only: approve, reject (optionally saying why), or put back in the queue. */
    suspend fun moderateBay(id: String, status: ModerationStatus, reason: String? = null)
    suspend fun moderatePhoto(id: String, status: ModerationStatus, reason: String? = null)
    suspend fun moderateComment(id: String, status: ModerationStatus, reason: String? = null)

    /** Admin only: remove for good, whatever its status. */
    suspend fun deleteBay(id: String)
    suspend fun deletePhoto(id: String)
    suspend fun deleteComment(id: String)

    /** Sets the signed-in rider's star rating of [bay] (1 to 5), replacing any earlier one. */
    suspend fun rate(bay: Bay, stars: Int)

    /** Takes back the signed-in rider's rating of [bay]. */
    suspend fun removeRating(bay: Bay)

    /** Every photo, comment, bay change and rating the signed-in rider ever submitted, and the photo files. */
    suspend fun deleteEverythingByMe()

    /** Flags someone else's content for the admin. [reason] is a key from [Legal.REPORT_REASONS]. */
    suspend fun report(target: ReportTarget, targetId: String, targetAuthorUid: String, reason: String, details: String)
}

enum class ReportTarget(val key: String) { Photo("photo"), Comment("comment"), Bay("bay") }
