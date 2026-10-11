package dem.dev.timeflame.data.repository

import dem.dev.timeflame.data.dto.Response
import dem.dev.timeflame.data.dto.UserDto
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.domain.repository.UserRepository
import dem.dev.timeflame.util.restApiConfig.BackendEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.request

class UserRepositoryImpl(
    private val httpClient: HttpClient
): UserRepository {
    override suspend fun getUserById(userId: String): Result<User> {
        try {
            val response = httpClient.get(BackendEndpoints.User.getById) {
                parameter("id", userId)
            }.body<Response<UserDto>>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun resetUserPasswordByEmail(email: String): Result<Unit> {
        try {
            val response = httpClient.post(BackendEndpoints.User.resetPassword) {
                parameter("email", email)
            }.body<Response<Nothing>>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun saveUserDeviceToken(userId: String, deviceToken: String): Result<Unit> {
        try {
            val response = httpClient.post(BackendEndpoints.User.devicesTokens(userId)) {
                parameter("token", deviceToken)
            }
            return Result(response.status.value)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }
}