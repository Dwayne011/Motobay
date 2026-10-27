package uk.motobay.app.data

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

sealed interface ClaimResult {
    data object Ok : ClaimResult
    data object Taken : ClaimResult
    data class Invalid(val why: String) : ClaimResult
    data object Failed : ClaimResult
}

/**
 * Display names are claimed in a `displayNames` collection keyed by the
 * lower-case name, so two riders can't share one. The rules only accept
 * submissions whose author is the rider's claimed name (or "Anonymous rider").
 */
interface DisplayNames {
    /** Takes [name] for the signed-in rider, releasing any name they held before. */
    suspend fun claim(name: String): ClaimResult

    /** The name this rider claimed earlier, e.g. on another phone. */
    suspend fun current(): String?

    companion object {
        const val ANONYMOUS = "Anonymous rider"
        const val MIN = 2
        const val MAX = 40
        private val ALLOWED = Regex("^[\\p{L}\\p{N} ._'-]+$")

        fun key(name: String): String = name.trim().lowercase(Locale.ROOT)

        /** Null when the name is fine, otherwise what's wrong with it. */
        fun problem(name: String): String? {
            val n = name.trim()
            return when {
                n.length < MIN -> "At least $MIN characters"
                n.length > MAX -> "At most $MAX characters"
                !ALLOWED.matches(n) -> "Letters, numbers, spaces and . _ ' - only"
                key(n) == key(ANONYMOUS) -> "That one's reserved"
                else -> null
            }
        }
    }
}

class FirestoreDisplayNames(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FirestoreCommunityRepository.DATABASE_ID),
) : DisplayNames {
    private val names get() = db.collection(DISPLAY_NAMES)

    override suspend fun claim(name: String): ClaimResult {
        val uid = auth.user.value?.uid ?: return ClaimResult.Failed
        DisplayNames.problem(name)?.let { return ClaimResult.Invalid(it) }
        val trimmed = name.trim()
        val newRef = names.document(DisplayNames.key(trimmed))
        return runCatching {
            db.runTransaction { tx ->
                val existing = tx.get(newRef)
                if (existing.exists() && existing.getString("uid") != uid) throw NameTaken()
                tx.set(newRef, mapOf("uid" to uid, "name" to trimmed, "createdAt" to FieldValue.serverTimestamp()))
            }.await()
            // Release the old name afterwards; a stale leftover is harmless but tidy to drop.
            names.whereEqualTo("uid", uid).get().await().documents
                .filter { it.id != newRef.id }
                .forEach { runCatching { it.reference.delete().await() } }
            ClaimResult.Ok
        }.getOrElse { e -> if (e is NameTaken || e.cause is NameTaken) ClaimResult.Taken else ClaimResult.Failed }
    }

    override suspend fun current(): String? {
        val uid = auth.user.value?.uid ?: return null
        return runCatching {
            names.whereEqualTo("uid", uid).limit(1).get().await().documents.firstOrNull()?.getString("name")
        }.getOrNull()
    }

    private class NameTaken : RuntimeException()

    private companion object {
        const val DISPLAY_NAMES = "displayNames"
    }
}
