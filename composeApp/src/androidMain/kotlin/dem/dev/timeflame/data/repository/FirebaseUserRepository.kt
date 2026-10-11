package dem.dev.timeflame.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task as GoogleTask
import com.google.firebase.auth.FirebaseAuth
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.domain.repository.UserRepository
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** User profile and password reset come from Firebase until v2 profile endpoints exist. */
class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserRepository {
    override suspend fun getUserById(userId: String): Result<User> {
        val current = auth.currentUser ?: return Result(ResponseCode.wrongCredentials, null)
        if (current.uid != userId) return Result(ResponseCode.wrongCredentials, null)
        return Result(
            ResponseCode.ok,
            User(id = current.uid, name = current.displayName ?: "", email = current.email ?: "")
        )
    }

    override suspend fun resetUserPasswordByEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).awaitFirebase().let {
            Result(ResponseCode.ok, Unit)
        }
    } catch (error: Exception) {
        Log.w("GenesisAuth", "Password reset failed: ${error.javaClass.simpleName}")
        Result(ResponseCode.badRequest, null)
    }

    override suspend fun saveUserDeviceToken(userId: String, deviceToken: String): Result<Unit> {
        // No FCM-token endpoint exists in API v2 yet; do not claim token persistence.
        return Result(ResponseCode.badRequest, null)
    }
}

private suspend fun <T> GoogleTask<T>.awaitFirebase(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { completed ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (completed.isSuccessful) {
                continuation.resume(completed.result)
            } else {
                continuation.resumeWithException(
                    completed.exception ?: IllegalStateException("Firebase operation failed")
                )
            }
        }
    }
