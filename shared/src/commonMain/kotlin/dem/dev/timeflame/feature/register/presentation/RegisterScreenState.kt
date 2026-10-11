package dem.dev.timeflame.feature.register.presentation

import dem.dev.timeflame.feature.register.navigation.RegisterScreenNavigationOptions
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.state.UiMessageCodes

data class UsernameFieldState(
    val value: String = "",
    val error: String? = null
)

data class EmailFieldState(
    val value: String = "",
    val error: String? = null
)

data class PasswordFieldState(
    val value: String = "",
    val showPassword: Boolean = false,
    val passwordStrength: Int = 0,
    val passwordStrengthActionCode: Int = UiMessageCodes.startTypingYourPassword,
    val error: String? = null
)

data class RegisterScreenState(
    val screenState: ScreenState = ScreenState.Idle,
    val usernameState: UsernameFieldState = UsernameFieldState(),
    val emailState: EmailFieldState = EmailFieldState(),
    val passwordState: PasswordFieldState = PasswordFieldState(),
    val navigateTo: RegisterScreenNavigationOptions? = null
)
