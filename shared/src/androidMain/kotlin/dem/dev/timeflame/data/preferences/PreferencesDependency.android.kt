package dem.dev.timeflame.data.preferences

import android.app.Application
import org.koin.dsl.module

actual typealias PreferencesDependency = Application
actual fun nativeAppModule(dep: PreferencesDependency) = preferenceModule(dep)

fun preferenceModule(app: Application) = module {
    factory { KmpPreference(app) }
}