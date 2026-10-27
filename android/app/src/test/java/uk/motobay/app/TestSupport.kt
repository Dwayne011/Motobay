package uk.motobay.app

import android.app.Activity
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.motobay.app.data.AuthRepository
import uk.motobay.app.data.AuthUser
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayComment
import uk.motobay.app.data.BayPhoto
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.CommunityData
import uk.motobay.app.data.CommunityRepository
import uk.motobay.app.data.ModerationStatus
import java.util.UUID

/** Stands in for Firebase Auth in tests. Starts signed out. */
class FakeAuthRepository : AuthRepository {
    private val state = MutableStateFlow<AuthUser?>(null)
    override val user: StateFlow<AuthUser?> = state.asStateFlow()

    fun set(user: AuthUser?) { state.value = user }

    override suspend fun signInWithGoogle(activity: Activity) = set(RIDER)
    override suspend fun signInWithApple(activity: Activity) = set(RIDER)
    override fun signOut() = set(null)

    companion object {
        val RIDER = AuthUser("rider1", "rider@example.com", isModerator = false)
        val ADMIN = AuthUser("admin1", "dwayne.edwards89@googlemail.com", isModerator = true)
    }
}

/** In-memory stand-in for Firestore in tests. Like the real one, it needs a signed-in rider to submit. */
class FakeCommunityRepository(private val auth: AuthRepository) : CommunityRepository {
    private val state = MutableStateFlow(CommunityData())
    override val data: StateFlow<CommunityData> = state.asStateFlow()

    private fun uid() = auth.user.value?.uid ?: throw IllegalStateException("Sign in to contribute")
    private fun now() = System.currentTimeMillis()

    override suspend fun submitPhoto(bay: Bay, source: Uri, caption: String, author: String) {
        val photo = BayPhoto(UUID.randomUUID().toString(), bay.id, bay.name, source.toString(), caption, author, now(), ModerationStatus.Pending, uid())
        state.update { it.copy(photos = listOf(photo) + it.photos) }
    }

    override suspend fun submitComment(bay: Bay, text: String, author: String) {
        val comment = BayComment(UUID.randomUUID().toString(), bay.id, bay.name, text, author, now(), ModerationStatus.Pending, uid())
        state.update { it.copy(comments = listOf(comment) + it.comments) }
    }

    override suspend fun submitBay(submission: BaySubmission) {
        val s = submission.copy(id = UUID.randomUUID().toString(), createdAt = now(), authorUid = uid(), status = ModerationStatus.Pending)
        state.update { it.copy(bays = listOf(s) + it.bays) }
    }

    override suspend fun moderateBay(id: String, status: ModerationStatus) =
        state.update { d -> d.copy(bays = d.bays.map { if (it.id == id) it.copy(status = status) else it }) }

    override suspend fun moderatePhoto(id: String, status: ModerationStatus) =
        state.update { d -> d.copy(photos = d.photos.map { if (it.id == id) it.copy(status = status) else it }) }

    override suspend fun moderateComment(id: String, status: ModerationStatus) =
        state.update { d -> d.copy(comments = d.comments.map { if (it.id == id) it.copy(status = status) else it }) }

    override suspend fun deleteBay(id: String) = state.update { d -> d.copy(bays = d.bays.filterNot { it.id == id }) }
    override suspend fun deletePhoto(id: String) = state.update { d -> d.copy(photos = d.photos.filterNot { it.id == id }) }
    override suspend fun deleteComment(id: String) = state.update { d -> d.copy(comments = d.comments.filterNot { it.id == id }) }
}

/** Runs the real screens against the fakes above, so tests never touch Firebase. */
class TestMotoBayApplication : MotoBayApplication() {
    override fun useSampleData() = true
    override fun createAuth(): AuthRepository = FakeAuthRepository()
    override fun createCommunity(auth: AuthRepository): CommunityRepository = FakeCommunityRepository(auth)
}
