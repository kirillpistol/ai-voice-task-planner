package dem.dev.timeflame.domain.repository

import dem.dev.timeflame.domain.model.AuthenticationRequest
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.RegistrationRequest
import dem.dev.timeflame.domain.model.Result

interface AuthRepository {
    suspend fun login(authenticationRequest: AuthenticationRequest): Result<AuthenticationResponse>
    suspend fun register(registrationRequest: RegistrationRequest): Result<AuthenticationResponse>
}