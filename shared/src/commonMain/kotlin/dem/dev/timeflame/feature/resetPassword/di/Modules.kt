package dem.dev.timeflame.feature.resetPassword.di

import dem.dev.timeflame.domain.repository.UserRepository
import dem.dev.timeflame.feature.resetPassword.usecase.ResetPasswordUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

fun resetPasswordFeatureModule() = listOf(useCaseModule)

private val useCaseModule = module {
    single<ResetPasswordUseCase> { ResetPasswordUseCase(get<UserRepository>()) }
}

class ResetPasswordUseCaseWrapper: KoinComponent {
    val useCase: ResetPasswordUseCase by inject<ResetPasswordUseCase>()
}