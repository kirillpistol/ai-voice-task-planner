package dem.dev.timeflame.feature.register.presentation

import dem.dev.timeflame.feature.login.presentation.LoginScreenEvent

sealed interface RegisterScreenEvent {
    data class NameChanged(val newName: String): RegisterScreenEvent
    data class EmailChanged(val newEmail: String): RegisterScreenEvent
    data class PasswordChanged(val newPassword: String): RegisterScreenEvent
    data object TogglePasswordVisibilityBtnClicked: RegisterScreenEvent
    data object RegisterBtnClicked: RegisterScreenEvent
    data object ToLoginClicked: RegisterScreenEvent
    data object MessageDialogDismissed: RegisterScreenEvent
}