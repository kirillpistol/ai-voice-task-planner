package dem.dev.timeflame.feature.resetPassword.usecase

import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.UserRepository

class ResetPasswordUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return userRepository.resetUserPasswordByEmail(email)
    }
}