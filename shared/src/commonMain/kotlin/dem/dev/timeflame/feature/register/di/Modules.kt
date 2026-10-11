package dem.dev.timeflame.feature.register.di

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.repository.AuthRepository
import dem.dev.timeflame.feature.register.usecase.RegisterUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.dsl.module

fun registerFeatureModule() = listOf(viewModelModule, useCaseModule)

internal expect val viewModelModule: Module

private val useCaseModule = module {
    single<RegisterUseCase> { RegisterUseCase(get<AuthRepository>(), get<LocalAuthManager>()) }
}
