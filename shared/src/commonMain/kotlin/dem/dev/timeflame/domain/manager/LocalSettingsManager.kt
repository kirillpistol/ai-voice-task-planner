package dem.dev.timeflame.domain.manager

import dem.dev.timeflame.domain.model.LocalSettings

interface LocalSettingsManager {
    fun getCurrentSettings(): LocalSettings
    fun saveSettings(localSettings: LocalSettings)
}