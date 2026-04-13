package dem.dev.timeflame.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Login: Screen

    @Serializable
    data object ResetPassword: Screen

    @Serializable
    data object Register: Screen

    @Serializable
    data object Main: Screen
}