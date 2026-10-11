package dem.dev.timeflame.feature.login.presentation

sealed interface LoginScreenEvent {
    data class EmailChanged(val newEmail: String): LoginScreenEvent
    data class PasswordChanged(val newPassword: String): LoginScreenEvent
    data object TogglePasswordVisibilityBtnClicked: LoginScreenEvent
    data object LoginBtnClicked: LoginScreenEvent
    data object ResetPasswordClicked: LoginScreenEvent
    data object CreateAccountClicked: LoginScreenEvent
    data object MessageDialogDismissed: LoginScreenEvent
}