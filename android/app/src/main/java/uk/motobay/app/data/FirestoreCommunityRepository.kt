package uk.motobay.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Community content in Cloud Firestore, with photos in Cloud Storage. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCommunityRepository(
    private val context: Context,
    private val auth: AuthRepository,
    scope: CoroutineScope,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) : CommunityRepository {

    private data class Viewer(val uid: String, val moderator: Boolean)

    override val data: StateFlow<CommunityData> = auth.user
        .map { u -> u?.let { Viewer(it.uid, it.isModerator) } }
        .distinctUntilChanged()
        .flatMapLatest { viewer ->
            combine(
                listen(PHOTOS, viewer, ::photoFrom),
                listen(COMMENTS, viewer, ::commentFrom),
                listen(BAYS, viewer, ::baySubmissionFrom),
            ) { photos, comments, bays ->
                CommunityData(
                    photos = photos.sortedByDescending { it.createdAt },
                    comments = comments.sortedByDescending { it.createdAt },
                    bays = bays.sortedByDescending { it.createdAt },
                )
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, CommunityData())

    /**
     * Firestore rules are not filters, so each viewer asks only for what they may
     * read: the admin everything, a rider approved items plus their own, anyone
     * else approved items.
     */
    private fun <T> listen(collection: String, viewer: Viewer?, parse: (String, Map<String, Any?>) -> T?): Flow<List<T>> {
        val col = db.collection(collection)
        val approved = col.whereEqualTo("status", ModerationStatus.Approved.name)
        val queries = when {
            viewer == null -> listOf(approved)
            viewer.moderator -> listOf(col)
            else -> listOf(approved, col.whereEqualTo("authorUid", viewer.uid))
        }
        return combine(queries.map { q -> q.snapshotFlow(parse) }) { lists ->
            lists.fold(emptyMap<String, T>()) { acc, l -> acc + l }.values.toList()
        }
    }

    private fun <T> Query.snapshotFlow(parse: (String, Map<String, Any?>) -> T?): Flow<Map<String, T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listener failed: ${error.message}")
                trySend(emptyMap())
            } else if (snapshot != null) {
                val docs = snapshot.documents.mapNotNull { d ->
                    d.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { parse(d.id, it) }?.let { d.id to it }
                }
                trySend(docs.toMap())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun submitPhoto(bay: Bay, source: Uri, caption: String, author: String) {
        val uid = requireUid()
        val id = db.collection(PHOTOS).document().id
        val path = "photos/$uid/$id.jpg"
        val bytes = withContext(Dispatchers.IO) { downscaleToJpeg(context, source) }
        val ref = storage.reference.child(path)
        ref.putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        try {
            val url = ref.downloadUrl.await().toString()
            db.collection(PHOTOS).document(id).set(photoToMap(bay, url, path, caption, author, uid)).awaitQueued()
        } catch (e: Exception) {
            runCatching { ref.delete().await() } // riders can't delete; the admin can clean up leftovers
            throw e
        }
    }

    override suspend fun submitComment(bay: Bay, text: String, author: String) {
        val uid = requireUid()
        db.collection(COMMENTS).document().set(commentToMap(bay, text, author, uid)).awaitQueued()
    }

    override suspend fun submitBay(submission: BaySubmission) {
        val uid = requireUid()
        db.collection(BAYS).document().set(baySubmissionToMap(submission, uid, ModerationStatus.Pending)).awaitQueued()
    }

    override suspend fun moderateBay(id: String, status: ModerationStatus, reason: String?) = moderate(BAYS, id, status, reason)
    override suspend fun moderatePhoto(id: String, status: ModerationStatus, reason: String?) = moderate(PHOTOS, id, status, reason)
    override suspend fun moderateComment(id: String, status: ModerationStatus, reason: String?) = moderate(COMMENTS, id, status, reason)

    private suspend fun moderate(collection: String, id: String, status: ModerationStatus, reason: String?) {
        db.collection(collection).document(id).update(decisionToMap(status, reason)).awaitQueued()
    }

    override suspend fun deleteBay(id: String) = db.collection(BAYS).document(id).delete().awaitQueued()
    override suspend fun deleteComment(id: String) = db.collection(COMMENTS).document(id).delete().awaitQueued()

    /** The document first (that is what hides the photo), then the file; a leftover file is harmless. */
    override suspend fun deletePhoto(id: String) {
        val path = data.value.photos.firstOrNull { it.id == id }?.storagePath
        db.collection(PHOTOS).document(id).delete().awaitQueued()
        if (!path.isNullOrEmpty()) runCatching { storage.reference.child(path).delete().await() }
            .onFailure { Log.w(TAG, "Couldn't delete $path: ${it.message}") }
    }

    private fun requireUid(): String = auth.user.value?.uid ?: throw IllegalStateException("Sign in to contribute")

    /**
     * Firestore finishes a write task only once the server has confirmed it, which
     * never happens offline. The write is queued and syncs later, so after a
     * while stop waiting; a rejected write still fails quickly when online.
     */
    private suspend fun <T> Task<T>.awaitQueued() {
        withTimeoutOrNull(WRITE_WAIT_MS) { await() }
    }

    companion object {
        /** The project's Firestore database is named, not "(default)". Keep in sync with firebase.json. */
        const val DATABASE_ID = "motobays"
        private const val TAG = "Community"
        private const val WRITE_WAIT_MS = 15_000L
    }
}
