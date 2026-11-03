package uk.motobay.app.ui.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import uk.motobay.app.data.SignInCancelledException
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.LocalSnackbar

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Continue with Google / Apple. */
@Composable
fun SignInButtons(modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun signIn(block: suspend (Activity) -> Unit) {
        val activity = context.findActivity() ?: return
        busy = true
        scope.launch {
            try {
                block(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SignInCancelledException) {
                // The rider backed out; nothing to say.
            } catch (e: Exception) {
                snackbar.showSnackbar("Couldn't sign in. Please try again.")
            } finally {
                busy = false
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !busy, onClick = { signIn { app.auth.signInWithGoogle(it) } }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Google")
        }
        OutlinedButton(enabled = !busy, onClick = { signIn { app.auth.signInWithApple(it) } }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Apple")
        }
    }
}

/** Shown when a signed-out rider tries to submit something. Closes itself once they're in. */
@Composable
fun SignInDialog(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val user by app.auth.user.collectAsStateWithLifecycle()
    LaunchedEffect(user) { if (user != null) onDismiss() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign in to contribute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Photos, comments and bay changes need an account. Everything is checked by a moderator before it's published.")
                SignInButtons()
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
