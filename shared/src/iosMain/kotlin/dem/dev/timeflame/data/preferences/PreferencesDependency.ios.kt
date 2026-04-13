package dem.dev.timeflame.data.preferences

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.darwin.NSObject

actual typealias PreferencesDependency = NSObject
actual fun nativeAppModule(dep: PreferencesDependency): Module = preferenceModule

val preferenceModule = module {
    factory { KmpPreference(NSObject()) }
}