package dem.dev.timeflame.data.repository

import dem.dev.timeflame.data.dto.AuthenticationRequestDto
import dem.dev.timeflame.data.dto.AuthenticationResponseDto
import dem.dev.timeflame.data.dto.Response
import dem.dev.timeflame.data.util.toDto
import dem.dev.timeflame.domain.model.AuthenticationRequest
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.RegistrationRequest
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.AuthRepository
import dem.dev.timeflame.util.restApiConfig.BackendEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthRepositoryImpl(
    private val httpClient: HttpClient
): AuthRepository {
    override suspend fun login(authenticationRequest: AuthenticationRequest): Result<AuthenticationResponse> {
        try {
            val response = httpClient.post(BackendEndpoints.Auth.login) {
                contentType(ContentType.Application.Json)
                setBody(authenticationRequest.toDto())
            }.body<Response<AuthenticationResponseDto>>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun register(registrationRequest: RegistrationRequest): Result<AuthenticationResponse> {
        try {
            val response = httpClient.post(BackendEndpoints.Auth.register) {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest.toDto())
            }.body<Response<AuthenticationResponseDto>>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }
}