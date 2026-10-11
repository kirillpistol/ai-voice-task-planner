package dem.dev.timeflame.feature.login.presentation

import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.feature.login.usecase.LoginUseCase
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.state.UiMessageCodes
import dem.dev.timeflame.util.viewModel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginScreenViewModel(
    private val loginUseCase: LoginUseCase,
    private val localAuthManager: LocalAuthManager
): BaseViewModel() {
    private val _state = MutableStateFlow(LoginScreenState())
    val state = _state.asStateFlow()

    fun onEvent(event: LoginScreenEvent) {
        when (event) {
            LoginScreenEvent.CreateAccountClicked -> onCreateAccountClicked()
            is LoginScreenEvent.EmailChanged -> onEmailChanged(event.newEmail)
            is LoginScreenEvent.PasswordChanged -> onPasswordChanged(event.newPassword)
            LoginScreenEvent.ResetPasswordClicked -> onResetPasswordClicked()
            LoginScreenEvent.TogglePasswordVisibilityBtnClicked -> onTogglePasswordVisibilityBtnClicked()
            LoginScreenEvent.LoginBtnClicked -> onLoginBtnClicked()
            LoginScreenEvent.MessageDialogDismissed -> onMessageDialogDismissed()
        }
    }

    private fun checkLogin() {
        if (localAuthManager.getCurrentUser() != null)
            _state.update { it.copy(navigateTo = LoginScreenNavigateOption.MainScreen) }
    }

    private fun login(email: String, password: String) = scope.launch {
        _state.update { it.copy(screenState = ScreenState.Loading(messageCode = UiMessageCodes.loggingIn)) }

        val result = loginUseCase(email, password)

        if (result.isSuccess())
            _state.update { it.copy(navigateTo = LoginScreenNavigateOption.MainScreen) }
        else
            _state.update { it.copy(
                screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorLoggingInCheckYourCred)
            ) }
    }

    private fun onMessageDialogDismissed() {
        _state.update { it.copy(screenState = ScreenState.Idle)}
    }
    private fun onLoginBtnClicked() {
        if (_state.value.email.value.isEmpty() || _state.value.password.value.isEmpty()) {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.fieldsCannotBeEmpty)) }
            return
        }
        login(_state.value.email.value, _state.value.password.value)
    }
    private fun onCreateAccountClicked() {
        _state.update { it.copy(navigateTo = LoginScreenNavigateOption.RegisterScreen) }
    }
    private fun onEmailChanged(email: String) {
        _state.update { it.copy(email = it.email.copy(value = email)) }
    }
    private fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = it.password.copy(value = password)) }
    }
    private fun onResetPasswordClicked() {
        _state.update { it.copy(navigateTo = LoginScreenNavigateOption.ResetPasswordScreen) }
    }
    private fun onTogglePasswordVisibilityBtnClicked() {
        _state.update { it.copy(password = it.password.copy(showPassword = !it.password.showPassword)) }
    }

    fun clearState() {
        _state.update { it.copy(navigateTo = null) }
    }

    init {
        checkLogin()
    }
}