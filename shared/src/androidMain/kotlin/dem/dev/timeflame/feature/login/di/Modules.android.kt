package dem.dev.timeflame.feature.login.di

import dem.dev.timeflame.feature.login.presentation.LoginScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal actual val viewModelModule = module {
    viewModelOf(::LoginScreenViewModel)
}