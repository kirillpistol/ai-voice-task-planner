package dem.dev.timeflame.domain.repository

import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.User

interface UserRepository {
    suspend fun getUserById(userId: String): Result<User>
    suspend fun resetUserPasswordByEmail(email: String): Result<Unit>
    suspend fun saveUserDeviceToken(userId: String, deviceToken: String): Result<Unit>
}