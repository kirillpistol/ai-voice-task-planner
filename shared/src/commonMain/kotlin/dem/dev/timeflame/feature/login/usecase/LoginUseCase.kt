package dem.dev.timeflame.feature.login.usecase

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.model.AuthenticationRequest
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.LocalUser
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val localAuthManager: LocalAuthManager
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthenticationResponse> {
        val authenticationRequest = AuthenticationRequest(email, password)
        val res = authRepository.login(authenticationRequest)

        // saving user login locally
        if (res.isSuccess() && res.data != null)
            localAuthManager.authorize(LocalUser(res.data.token, res.data.id))
        return res
    }
}