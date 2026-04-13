package dem.dev.timeflame.feature.register.usecase

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.LocalUser
import dem.dev.timeflame.domain.model.RegistrationRequest
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val localAuthManager: LocalAuthManager
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<AuthenticationResponse> {
        val registrationRequest = RegistrationRequest(name, email, password)
        val res = authRepository.register(registrationRequest)

        //saving user locally
        if (res.isSuccess() && res.data != null)
            localAuthManager.authorize(LocalUser(res.data.token, res.data.id))

        return res
    }
}