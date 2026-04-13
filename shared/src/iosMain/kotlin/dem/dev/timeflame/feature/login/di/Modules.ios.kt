package dem.dev.timeflame.feature.login.di

import dem.dev.timeflame.feature.login.presentation.LoginScreenViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual val viewModelModule = module {
    singleOf(::LoginScreenViewModel)
}