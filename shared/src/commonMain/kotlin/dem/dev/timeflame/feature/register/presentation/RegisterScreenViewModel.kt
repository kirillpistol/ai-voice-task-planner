package dem.dev.timeflame.feature.register.presentation

import dem.dev.timeflame.feature.register.navigation.RegisterScreenNavigationOptions
import dem.dev.timeflame.feature.register.usecase.RegisterUseCase
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.state.UiMessageCodes
import dem.dev.timeflame.util.viewModel.BaseViewModel
import dem.dev.timeflame.util.viewModel.containsSpecialCharacters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterScreenViewModel(
    private val registerUseCase: RegisterUseCase
): BaseViewModel() {
    private val _state = MutableStateFlow(RegisterScreenState())
    val state = _state.asStateFlow()

    fun onEvent(event: RegisterScreenEvent) {
        when(event) {
            is RegisterScreenEvent.EmailChanged -> onEmailChanged(event.newEmail)
            RegisterScreenEvent.MessageDialogDismissed -> onMessageDialogDismissed()
            is RegisterScreenEvent.NameChanged -> onNameChanged(event.newName)
            is RegisterScreenEvent.PasswordChanged -> onPasswordChanged(event)
            RegisterScreenEvent.RegisterBtnClicked -> onRegisterBtnClicked()
            RegisterScreenEvent.ToLoginClicked -> onToLoginClicked()
            RegisterScreenEvent.TogglePasswordVisibilityBtnClicked -> onTogglePasswordVisibilityBtnClicked()
        }
    }

    private fun onEmailChanged(email: String) {
        _state.update { it.copy(emailState = it.emailState.copy(value = email)) }
    }
    private fun onMessageDialogDismissed() {
        _state.update { it.copy(screenState = ScreenState.Idle) }
    }
    private fun onNameChanged(name: String) {
        _state.update { it.copy(usernameState = it.usernameState.copy(value = name)) }
    }
    private fun onPasswordChanged(event: RegisterScreenEvent.PasswordChanged) {
        val passwordStrengthLevel =
            if (event.newPassword.isEmpty()) 0
            else if(event.newPassword.length < 5) 1
            else if (!event.newPassword.any { it.isUpperCase() }) 2
            else if (!event.newPassword.containsSpecialCharacters()) 3
            else 4

        val actionCode = when(passwordStrengthLevel) {
            1 -> UiMessageCodes.addAtLeast6Symbols
            2 -> UiMessageCodes.passwordNeedsToContainBothSmallAndLarge
            3 -> UiMessageCodes.addSpecialLetters
            4 -> UiMessageCodes.allGood
            else -> UiMessageCodes.startTypingYourPassword
        }

        _state.update {
            it.copy(
                passwordState = it.passwordState.copy(
                    value = event.newPassword,
                    passwordStrength = passwordStrengthLevel,
                    passwordStrengthActionCode = actionCode
                )
            )
        }
    }
    private fun onRegisterBtnClicked() {
        if (_state.value.usernameState.value.isEmpty() || _state.value.emailState.value.isEmpty() || _state.value.passwordState.value.isEmpty()) {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.fieldsCannotBeEmpty)) }
            return
        }
        if (_state.value.passwordState.passwordStrength < 3) {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.passwordStrengthLevel3Needed)) }
            return
        }
        register(
            name = _state.value.usernameState.value,
            email = _state.value.emailState.value,
            password = _state.value.passwordState.value
        )
    }
    private fun onToLoginClicked() {
        _state.update { it.copy(navigateTo = RegisterScreenNavigationOptions.LoginScreen) }
    }
    private fun onTogglePasswordVisibilityBtnClicked() {
        _state.update { it.copy(passwordState = it.passwordState.copy(showPassword = !it.passwordState.showPassword)) }
    }

    private fun register(name: String, email: String, password: String) = scope.launch {
        if (email.isEmpty())

        _state.update { it.copy(screenState = ScreenState.Loading(UiMessageCodes.creatingYourAccount)) }

        withContext(Dispatchers.IO) {
            val result = registerUseCase(name, email, password)

            if (result.isSuccess())
                _state.update { it.copy(navigateTo = RegisterScreenNavigationOptions.MainScreen) }
            else
                _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorWhileCreatingAccount)) }
        }
    }
}