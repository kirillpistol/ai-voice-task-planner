package dem.dev.timeflame.di

import dem.dev.timeflame.feature.resetPassword.ResetPasswordScreenViewModel
import dem.dev.timeflame.feature.main.MainScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::ResetPasswordScreenViewModel)
    viewModelOf(::MainScreenViewModel)
}