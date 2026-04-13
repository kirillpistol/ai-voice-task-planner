package dem.dev.timeflame.feature.register.navigation

sealed interface RegisterScreenNavigationOptions {
    data object LoginScreen: RegisterScreenNavigationOptions
    data object MainScreen: RegisterScreenNavigationOptions
}