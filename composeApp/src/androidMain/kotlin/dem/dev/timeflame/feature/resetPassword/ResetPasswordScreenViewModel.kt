package dem.dev.timeflame.feature.resetPassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dem.dev.timeflame.feature.resetPassword.usecase.ResetPasswordUseCase
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.state.UiMessageCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordScreenViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase
): ViewModel() {
    private val _state = MutableStateFlow(ResetPasswordScreenState())
    val state = _state.asStateFlow()

    fun onEvent(event: ResetPasswordScreenEvent) {
        when(event) {
            is ResetPasswordScreenEvent.EmailChanged -> onEmailChanged(event.newEmail)
            ResetPasswordScreenEvent.SendNewPasswordBtnClicked -> onSendNewPasswordBtnClicked()
            ResetPasswordScreenEvent.MessageDialogDismissed -> onMessageDialogDismissed()
        }
    }

    private fun onEmailChanged(newEmail: String) {
        _state.update { it.copy(emailState = it.emailState.copy(value = newEmail)) }
    }
    private fun onSendNewPasswordBtnClicked() {
        _state.update { it.copy(screenState = ScreenState.Loading(UiMessageCodes.sendingNewPassword)) }
        sendNewPassword(email = _state.value.emailState.value)
    }
    private fun onMessageDialogDismissed() {
        _state.update { it.copy(screenState = ScreenState.Idle) }
    }

    private fun sendNewPassword(email: String) = viewModelScope.launch(Dispatchers.IO) {
        val res = resetPasswordUseCase(email)

        if (res.isSuccess())
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.SUCCESS, UiMessageCodes.passwordWasSentToYourEmail)) }
        else
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.thereIsNoUserWithThisEmail))}
    }
}