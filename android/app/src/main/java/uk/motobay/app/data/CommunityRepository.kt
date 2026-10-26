package uk.motobay.app.data

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class CommunityData(
    val photos: List<BayPhoto> = emptyList(),
    val comments: List<BayComment> = emptyList(),
    val bays: List<BaySubmission> = emptyList(),
)

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

    /**
     * A new bay, or a correction when [BaySubmission.editOf] is set. Only the
     * admin's own submissions can be auto-approved.
     */
    suspend fun submitBay(submission: BaySubmission, autoApprove: Boolean)

    /** Admin only. */
    suspend fun moderateBay(id: String, status: ModerationStatus)
    suspend fun moderatePhoto(id: String, status: ModerationStatus)
    suspend fun moderateComment(id: String, status: ModerationStatus)
}
