package dem.dev.timeflame.app

import android.app.Application
import dem.dev.timeflame.data.di.dataModules
import dem.dev.timeflame.data.preferences.nativeAppModule
import dem.dev.timeflame.di.viewModelModule
import dem.dev.timeflame.feature.calendar.di.calendarFeatureModule
import dem.dev.timeflame.feature.login.di.loginFeatureModules
import dem.dev.timeflame.feature.register.di.registerFeatureModule
import dem.dev.timeflame.feature.resetPassword.di.resetPasswordFeatureModule
import dem.dev.timeflame.feature.task.di.taskFeatureModule

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin()
    }

    private fun startKoin() {
        org.koin.core.context.startKoin {
            modules(nativeAppModule(this@App))
            modules(dataModules())
            modules(loginFeatureModules())
            modules(registerFeatureModule())
            modules(resetPasswordFeatureModule())
            modules(taskFeatureModule())
            modules(calendarFeatureModule())
            modules(viewModelModule)
        }
    }
}