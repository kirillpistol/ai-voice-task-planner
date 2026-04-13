package dem.dev.timeflame.feature.login.presentation

sealed interface LoginScreenNavigateOption {
    data object RegisterScreen: LoginScreenNavigateOption
    data object ResetPasswordScreen: LoginScreenNavigateOption
    data object MainScreen: LoginScreenNavigateOption
}