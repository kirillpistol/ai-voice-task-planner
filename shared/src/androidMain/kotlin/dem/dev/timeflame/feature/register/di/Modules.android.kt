package dem.dev.timeflame.feature.register.di

import dem.dev.timeflame.feature.register.presentation.RegisterScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal actual val viewModelModule = module {
    viewModelOf(::RegisterScreenViewModel)
}