package dem.dev.timeflame.data.manager

import dem.dev.timeflame.data.preferences.KEY_APP_LANG
import dem.dev.timeflame.data.preferences.KEY_IS_IN_DARK_THEME
import dem.dev.timeflame.data.preferences.KmpPreference
import dem.dev.timeflame.domain.manager.LocalSettingsManager
import dem.dev.timeflame.domain.model.AppLanguage
import dem.dev.timeflame.domain.model.LocalSettings

class LocalSettingsManagerImpl(
    private val kmpPreference: KmpPreference
): LocalSettingsManager {
    override fun getCurrentSettings(): LocalSettings {
        val isInDarkTheme = kmpPreference.getBool(KEY_IS_IN_DARK_THEME, false)
        val language = when(kmpPreference.getString(KEY_APP_LANG)) {
            AppLanguage.RUS.name -> AppLanguage.RUS
            AppLanguage.EN.name -> AppLanguage.EN
            else -> null
        } ?: return LocalSettings(false, AppLanguage.RUS)

        return LocalSettings(isInDarkTheme, language)
    }

    override fun saveSettings(localSettings: LocalSettings) {
        kmpPreference.put(KEY_IS_IN_DARK_THEME, localSettings.isInDarkTheme)
        kmpPreference.put(KEY_APP_LANG, localSettings.language.name)
    }
}