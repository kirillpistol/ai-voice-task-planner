package dem.dev.timeflame.feature.resetPassword

sealed interface ResetPasswordScreenEvent {
    data class EmailChanged(val newEmail: String): ResetPasswordScreenEvent
    data object SendNewPasswordBtnClicked: ResetPasswordScreenEvent
    data object MessageDialogDismissed: ResetPasswordScreenEvent
}