package dem.dev.timeflame.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dem.dev.timeflame.feature.login.LoginScreen
import dem.dev.timeflame.feature.main.MainScreen
import dem.dev.timeflame.feature.register.RegisterScreen
import dem.dev.timeflame.feature.resetPassword.presentation.ResetPasswordScreen

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Login
    ) {
        composable<Screen.Login> {
            LoginScreen(
                modifier = Modifier,
                navigateTo = {
                    navController.navigate(it)
                },
                clearBackStack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Screen.ResetPassword> {
            ResetPasswordScreen(
                modifier = Modifier,
                navigateTo = {
                    navController.navigate(it)
                }
            )
        }
        composable<Screen.Register> {
            RegisterScreen(
                modifier = Modifier,
                navigateTo = {
                    navController.navigate(it)
                }
            )
        }
        composable<Screen.Main> {
            MainScreen(
                navigateTo = {
                    navController.navigate(it)
                },
                popBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}