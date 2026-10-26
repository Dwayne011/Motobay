package uk.motobay.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class CommunityData(
    val photos: List<BayPhoto> = emptyList(),
    val comments: List<BayComment> = emptyList(),
    val bays: List<BaySubmission> = emptyList(),
)

/**
 * Photos, comments, new bays and bay corrections that riders submit.
 * Everything starts as Pending and is only shown publicly after a moderator
 * approves it.
 *
 * This is a local, on-device mock of the backend so the app can be built and
 * tested now. Replace the bodies of these functions with API calls once a
 * backend (for example Supabase or Firebase) is chosen; screens only use this
 * class, so they won't need to change.
 */
class CommunityRepository(private val context: Context, scope: CoroutineScope) {
    private val store = JsonFileStore(File(context.filesDir, "community.json"), CommunityData.serializer(), CommunityData(), scope)
    private val photoDir = File(context.filesDir, "photos").apply { mkdirs() }

    val data = store.state

    suspend fun submitPhoto(bay: Bay, source: Uri, caption: String, author: String) {
        val id = UUID.randomUUID().toString()
        val file = File(photoDir, "$id.jpg")
        withContext(Dispatchers.IO) { saveDownscaled(source, file) }
        val photo = BayPhoto(id, bay.id, bay.name, file.absolutePath, caption, author, System.currentTimeMillis(), ModerationStatus.Pending)
        store.update { it.copy(photos = listOf(photo) + it.photos) }
    }

    fun submitComment(bay: Bay, text: String, author: String) {
        val comment = BayComment(UUID.randomUUID().toString(), bay.id, bay.name, text, author, System.currentTimeMillis(), ModerationStatus.Pending)
        store.update { it.copy(comments = listOf(comment) + it.comments) }
    }

    /**
     * A new bay, or a correction when [editOf] is set. Moderators' own submissions
     * are approved straight away.
     */
    fun submitBay(submission: BaySubmission, autoApprove: Boolean) {
        val s = submission.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            status = if (autoApprove) ModerationStatus.Approved else ModerationStatus.Pending,
        )
        store.update { it.copy(bays = listOf(s) + it.bays) }
    }

    fun moderateBay(id: String, status: ModerationStatus) =
        store.update { d -> d.copy(bays = d.bays.map { if (it.id == id) it.copy(status = status) else it }) }

    fun moderatePhoto(id: String, status: ModerationStatus) =
        store.update { d -> d.copy(photos = d.photos.map { if (it.id == id) it.copy(status = status) else it }) }

    fun moderateComment(id: String, status: ModerationStatus) =
        store.update { d -> d.copy(comments = d.comments.map { if (it.id == id) it.copy(status = status) else it }) }

    /** Saves at most 1600px on the longest side as JPEG, which also drops EXIF location data. */
    private fun saveDownscaled(source: Uri, target: File, maxSize: Int = 1600) {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSize) {
                    val scale = maxSize.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val full = MediaStore.Images.Media.getBitmap(context.contentResolver, source)
            val scale = minOf(1f, maxSize.toFloat() / max(full.width, full.height))
            Bitmap.createScaledBitmap(full, (full.width * scale).roundToInt(), (full.height * scale).roundToInt(), true)
        }
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    }
}
