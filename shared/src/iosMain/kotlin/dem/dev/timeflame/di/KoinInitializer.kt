package dem.dev.timeflame.di

import dem.dev.timeflame.data.di.dataModules
import dem.dev.timeflame.data.preferences.nativeAppModule
import dem.dev.timeflame.feature.calendar.di.calendarFeatureModule
import dem.dev.timeflame.feature.login.di.loginFeatureModules
import dem.dev.timeflame.feature.login.presentation.LoginScreenViewModel
import dem.dev.timeflame.feature.register.di.registerFeatureModule
import dem.dev.timeflame.feature.resetPassword.di.resetPasswordFeatureModule
import dem.dev.timeflame.feature.task.di.taskFeatureModule
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.component.inject
import platform.darwin.NSObject

class LoginInjector: KoinComponent {
    val loginScreenViewModel: LoginScreenViewModel by inject()
}

fun initKoin() {
    startKoin {
        modules(nativeAppModule(NSObject()))
        modules(dataModules())
        modules(loginFeatureModules())
        modules(registerFeatureModule())
        modules(resetPasswordFeatureModule())
        modules(taskFeatureModule())
        modules(calendarFeatureModule())
    }
}