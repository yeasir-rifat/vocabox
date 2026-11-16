package com.vocabulary.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    /** Waiting for Firebase to say whether someone is already signed in. */
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val uid: String, val name: String, val email: String) : AuthState
}

/** Google sign-in on top of Firebase Authentication. The session survives app restarts and works offline. */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** False when google-services.json was not part of the build. */
    val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth? get() = if (isConfigured) FirebaseAuth.getInstance() else null

    val state: Flow<AuthState> = flow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            emitAll(flowOf(AuthState.SignedOut))
        } else {
            emitAll(callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toState()) }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            })
        }
    }

    val isSignedIn: Boolean get() = auth?.currentUser != null

    /** [activityContext] must be an Activity: the Google account picker is drawn over it. */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = try {
        val firebaseAuth = auth ?: throw SetupException("Firebase is not set up in this build.")
        val clientId = webClientId()
            ?: throw SetupException("Google sign-in is not enabled in the Firebase project.")

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Unexpected sign-in response."))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut() {
        auth?.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Signing out of Firebase is what matters; clearing the picker state is best effort.
        }
    }

    /** The id is generated from google-services.json, so it is looked up by name at run time. */
    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    private fun FirebaseUser?.toState(): AuthState =
        if (this == null) AuthState.SignedOut
        else AuthState.SignedIn(
            uid = uid,
            name = displayName.orEmpty().ifBlank { email.orEmpty().substringBefore('@') },
            email = email.orEmpty()
        )

    class SetupException(message: String) : Exception(message)
}
