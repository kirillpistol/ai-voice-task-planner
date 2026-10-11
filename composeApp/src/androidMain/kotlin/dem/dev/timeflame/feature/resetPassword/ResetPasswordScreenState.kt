package dem.dev.timeflame.feature.resetPassword

import dem.dev.timeflame.feature.login.presentation.EmailFieldState
import dem.dev.timeflame.navigation.Screen
import dem.dev.timeflame.util.state.ScreenState

data class ResetPasswordScreenState(
    val screenState: ScreenState = ScreenState.Idle,
    val emailState: EmailFieldState = EmailFieldState(),
    val navigateTo: Screen? = null
)
