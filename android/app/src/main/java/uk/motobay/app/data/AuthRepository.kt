package uk.motobay.app.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.motobay.app.R

/** The signed-in rider. [isModerator] is true only for the admin account. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val isModerator: Boolean,
)

/** The rider cancelled the sign-in screen; not an error worth showing. */
class SignInCancelledException : Exception()

/** Firebase wants a fresh sign-in before something sensitive like deleting the account. */
class RecentSignInRequiredException : Exception()

interface AuthRepository {
    /** Null when signed out. */
    val user: StateFlow<AuthUser?>

    suspend fun signInWithGoogle(activity: Activity)
    suspend fun signInWithApple(activity: Activity)
    fun signOut()

    /** Removes the Firebase user. Throws [RecentSignInRequiredException] if they must sign in again first. */
    suspend fun deleteAccount()
}

class FirebaseAuthRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {

    override val user: StateFlow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser?.toAuthUser())

    override suspend fun signInWithGoogle(activity: Activity) {
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Unexpected credential type ${credential.type}")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    /** Needs the Apple provider switched on in the Firebase console. */
    override suspend fun signInWithApple(activity: Activity) {
        val provider = OAuthProvider.newBuilder("apple.com").setScopes(listOf("email", "name")).build()
        try {
            val pending = auth.pendingAuthResult
            if (pending != null) pending.await() else auth.startActivityForSignInWithProvider(activity, provider).await()
        } catch (e: FirebaseAuthWebException) {
            if (e.errorCode == "ERROR_WEB_CONTEXT_CANCELED") throw SignInCancelledException()
            throw e
        }
    }

    /** Firebase session first, then Credential Manager, so the next sign-in shows the account picker again. */
    override fun signOut() {
        auth.signOut()
        scope.launch {
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        }
    }

    override suspend fun deleteAccount() {
        val user = auth.currentUser ?: return
        try {
            user.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw RecentSignInRequiredException()
        }
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    isModerator = Moderators.isModerator(
        email = email,
        emailVerified = isEmailVerified,
        viaGoogle = providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID },
    ),
)
