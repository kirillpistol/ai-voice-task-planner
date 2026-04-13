package dem.dev.timeflame.util.language

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object UiLanguageHelper {
    fun setLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(languageCode)
        else
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))

        saveLanguage(context, languageCode)
    }

    fun getCurrentLanguage(context: Context): String {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("lang", Locale.getDefault().language) ?: Locale.getDefault().language
    }

    fun getGoogleVoiceRecognitionLanguage(context: Context): String {
        val map = mapOf(
            "ru" to "ru-RU",
            "en" to "en-EN"
        )
        return map[context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("lang", Locale.getDefault().language) ?: Locale.getDefault().language] ?: "ru-RU"
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("lang", languageCode).apply()
    }
}