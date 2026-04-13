package dem.dev.timeflame.data.di

import dem.dev.timeflame.data.manager.LocalAuthManagerImpl
import dem.dev.timeflame.data.manager.LocalSettingsManagerImpl
import dem.dev.timeflame.data.preferences.KmpContext
import dem.dev.timeflame.data.preferences.KmpPreference
import dem.dev.timeflame.data.preferences.nativeAppModule
import dem.dev.timeflame.data.repository.AuthRepositoryImpl
import dem.dev.timeflame.data.repository.TaskRepositoryImpl
import dem.dev.timeflame.data.repository.UserRepositoryImpl
import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.manager.LocalSettingsManager
import dem.dev.timeflame.domain.repository.AuthRepository
import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun dataModules() = listOf(httpClientModule, repositoryModule, managerModule)

private val httpClientModule = module {
    factory(
        named("HttpClientWithBearer")
    ) {
        HttpClient {
            install(ContentNegotiation) {
                json(json = Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(
                            accessToken = get<LocalAuthManager>().getCurrentUser()?.token ?: "",
                            refreshToken = ""
                        )
                    }
                }
            }
        }
    }
    single(
        named("HttpClient")
    ) {
        HttpClient {
            install(ContentNegotiation) {
                json(json = Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
            }
        }
    }
}

private val repositoryModule = module {
    factory<AuthRepository> { AuthRepositoryImpl(get<HttpClient>(named("HttpClient"))) }
    factory<TaskRepository> { TaskRepositoryImpl(get<HttpClient>(named("HttpClientWithBearer"))) }
    factory<UserRepository> { UserRepositoryImpl(get<HttpClient>(named("HttpClientWithBearer"))) }
}

private val managerModule = module {
    factory<LocalAuthManager> { LocalAuthManagerImpl(get<KmpPreference>()) }
    factory<LocalSettingsManager> { LocalSettingsManagerImpl(get<KmpPreference>()) }
}