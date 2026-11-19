package uk.motobay.app.data

import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Riders' reports in Firestore. Each rider's own reports are private (spaceReports and
 * bayFeatures, one document per rider per bay); everyone reads the per-bay totals in
 * bayStatus, which the Cloud Function in functions/ keeps.
 */
class FirestoreBayStatusRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FirestoreCommunityRepository.DATABASE_ID),
) : BayStatusRepository {

    override fun status(bayId: String): Flow<BayStatus?> =
        db.collection(STATUS).document(docKey(bayId)).listen { it?.let(::statusFrom) }

    override fun mine(bayId: String): Flow<MyBayReport?> {
        val uid = auth.user.value?.uid ?: return flowOf(null)
        val id = reportId(uid, bayId)
        return kotlinx.coroutines.flow.combine(
            db.collection(SPACE).document(id).listen { it },
            db.collection(FEATURES).document(id).listen { it },
        ) { space, features ->
            MyBayReport(
                full = space?.get("full") as? Boolean,
                spaceAt = (space?.get("at") as? Timestamp)?.toDate()?.time,
                suspended = space?.get("suspended") == true,
                features = BayFeature.entries.mapNotNull { f -> (features?.get(f.key) as? Boolean)?.let { f to it } }.toMap(),
            )
        }
    }

    override suspend fun reportSpace(bay: Bay, full: Boolean) {
        val uid = requireUid()
        db.collection(SPACE).document(reportId(uid, bay.id)).set(
            mapOf("bayId" to bay.id, "authorUid" to uid, "full" to full, "at" to FieldValue.serverTimestamp()),
        ).awaitQueued()
    }

    override suspend fun reportSuspended(bay: Bay) {
        val uid = requireUid()
        db.collection(SPACE).document(reportId(uid, bay.id)).set(
            mapOf("bayId" to bay.id, "authorUid" to uid, "full" to true, "suspended" to true, "at" to FieldValue.serverTimestamp()),
        ).awaitQueued()
    }

    override suspend fun setFeature(bay: Bay, feature: BayFeature, value: Boolean?) {
        val uid = requireUid()
        db.collection(FEATURES).document(reportId(uid, bay.id)).set(
            mapOf(
                "bayId" to bay.id, "authorUid" to uid, "updatedAt" to FieldValue.serverTimestamp(),
                feature.key to (value ?: FieldValue.delete()),
            ),
            SetOptions.merge(),
        ).awaitQueued()
    }

    override suspend fun deleteMine() {
        val uid = requireUid()
        for (collection in listOf(SPACE, FEATURES)) {
            val docs = db.collection(collection).whereEqualTo("authorUid", uid).get().await().documents
            docs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
    }

    private fun <T> DocumentReference.listen(parse: (Map<String, Any?>?) -> T?): Flow<T?> = callbackFlow {
        val registration = addSnapshotListener { snap, error ->
            // A refused or failed listen just shows nothing; the bay page works without it.
            trySend(if (error != null) null else snap?.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).let(parse))
        }
        awaitClose { registration.remove() }
    }

    private fun requireUid(): String = auth.user.value?.uid ?: throw IllegalStateException("Sign in to report")

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitQueued() {
        withTimeoutOrNull(15_000L) { await() }
    }

    companion object {
        const val SPACE = "spaceReports"
        const val FEATURES = "bayFeatures"
        const val STATUS = "bayStatus"

        /** Document ids can't contain slashes. Matches summaryId in functions/summary.js. */
        fun docKey(bayId: String) = bayId.replace('/', '_')

        /** One report per rider per bay; firestore.rules checks the id. */
        fun reportId(uid: String, bayId: String) = uid + "_" + docKey(bayId)

        @Suppress("UNCHECKED_CAST")
        internal fun statusFrom(m: Map<String, Any?>): BayStatus {
            val space = (m["space"] as? Map<String, Any?>)?.let { s ->
                val at = (s["at"] as? Timestamp)?.toDate()?.time
                val full = s["full"] as? Boolean
                if (at != null && full != null) SpaceReport(full, at, s["suspended"] == true) else null
            }
            val features = (m["features"] as? Map<String, Any?>).orEmpty().let { f ->
                BayFeature.entries.mapNotNull { feature ->
                    (f[feature.key] as? Map<String, Any?>)?.let { v ->
                        Votes((v["yes"] as? Number)?.toInt() ?: 0, (v["no"] as? Number)?.toInt() ?: 0)
                    }?.takeIf { it.total > 0 }?.let { feature to it }
                }.toMap()
            }
            return BayStatus(space, features)
        }
    }
}
