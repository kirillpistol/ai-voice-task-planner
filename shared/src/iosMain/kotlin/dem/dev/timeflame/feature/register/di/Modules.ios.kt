package dem.dev.timeflame.feature.register.di

import dem.dev.timeflame.feature.register.presentation.RegisterScreenViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual val viewModelModule = module {
    singleOf(::RegisterScreenViewModel)
}