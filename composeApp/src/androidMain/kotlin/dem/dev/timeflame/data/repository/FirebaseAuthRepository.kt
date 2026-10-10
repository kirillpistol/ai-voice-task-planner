package dem.dev.timeflame.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dem.dev.timeflame.domain.model.AuthenticationRequest
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.RegistrationRequest
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.AuthRepository
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Android sign-in and sign-up use Firebase Auth, not the discontinued v1 server. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun login(authenticationRequest: AuthenticationRequest): Result<AuthenticationResponse> =
        try {
            val user = auth.signInWithEmailAndPassword(
                authenticationRequest.email.trim(),
                authenticationRequest.password
            ).awaitResult().user ?: error("Firebase returned no user")

            val idToken = user.getIdToken(false).awaitResult().token
                ?: error("Firebase returned no ID token")

            Result(ResponseCode.ok, AuthenticationResponse(idToken, user.uid))
        } catch (error: Exception) {
            Log.w("GenesisAuth", "Firebase sign-in failed: ${error.javaClass.simpleName}")
            Result(ResponseCode.wrongCredentials, null)
        }

    override suspend fun register(registrationRequest: RegistrationRequest): Result<AuthenticationResponse> =
        try {
            val user = auth.createUserWithEmailAndPassword(
                registrationRequest.email.trim(),
                registrationRequest.password
            ).awaitResult().user ?: error("Firebase returned no user")

            // A profile update should not make an already-created account appear to fail.
            try {
                val displayName = UserProfileChangeRequest.Builder()
                    .setDisplayName(registrationRequest.name.trim())
                    .build()
                user.updateProfile(displayName).awaitResult()
            } catch (error: Exception) {
                Log.w("GenesisAuth", "Profile update skipped: ${error.javaClass.simpleName}")
            }

            val idToken = user.getIdToken(false).awaitResult().token
                ?: error("Firebase returned no ID token")

            Result(ResponseCode.created, AuthenticationResponse(idToken, user.uid))
        } catch (error: Exception) {
            Log.w("GenesisAuth", "Firebase registration failed: ${error.javaClass.simpleName}")
            Result(ResponseCode.badRequest, null)
        }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { completed ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (completed.isSuccessful) {
                continuation.resume(completed.result)
            } else {
                continuation.resumeWithException(
                    completed.exception ?: IllegalStateException("Firebase request failed")
                )
            }
        }
    }
