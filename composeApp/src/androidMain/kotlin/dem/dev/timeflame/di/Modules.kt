package dem.dev.timeflame.di

import dem.dev.timeflame.feature.resetPassword.ResetPasswordScreenViewModel
import dem.dev.timeflame.feature.main.MainScreenViewModel
import dem.dev.timeflame.data.repository.GenesisWorkspaceClient
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    single { GenesisWorkspaceClient(get<HttpClient>(named("HttpClient"))) }
    viewModelOf(::ResetPasswordScreenViewModel)
    viewModelOf(::MainScreenViewModel)
}