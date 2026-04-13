package dem.dev.timeflame.data.preferences

import org.koin.core.module.Module

expect class PreferencesDependency
expect fun nativeAppModule(dep: PreferencesDependency): Module