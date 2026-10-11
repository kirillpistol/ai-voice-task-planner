package dem.dev.timeflame.feature.login.presentation

import dem.dev.timeflame.util.state.Loading
import dem.dev.timeflame.util.state.Result
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState

data class PasswordFieldState(
    val value: String = "",
    val showPassword: Boolean = false,
    val error: String? = null
)

data class EmailFieldState(
    val value: String = "",
    val error: String? = null
)

data class LoginScreenState(
    val screenState: ScreenState = ScreenState.Idle,
    val email: EmailFieldState = EmailFieldState(),
    val password: PasswordFieldState = PasswordFieldState(),
    val navigateTo: LoginScreenNavigateOption? = null
)
