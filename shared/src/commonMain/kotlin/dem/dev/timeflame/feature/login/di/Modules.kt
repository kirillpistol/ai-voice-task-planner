package dem.dev.timeflame.feature.login.di

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.repository.AuthRepository
import dem.dev.timeflame.feature.login.usecase.LoginUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

fun loginFeatureModules() = listOf(viewModelModule, useCaseModule)

internal expect val viewModelModule: Module

private val useCaseModule = module {
    single<LoginUseCase> { LoginUseCase(get<AuthRepository>(), get<LocalAuthManager>()) }
}