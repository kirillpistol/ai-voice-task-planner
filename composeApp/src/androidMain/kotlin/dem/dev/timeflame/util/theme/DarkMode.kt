package dem.dev.timeflame.util.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


object DarkModeHelper {
    @Composable
    fun isDarkModeEnabled(): Boolean? /* Returns null if user didn't choose yet */ {
        val sharedPreferences = LocalContext.current.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        return if (sharedPreferences.contains("darkMode"))
            sharedPreferences.getBoolean("darkMode", false)
        else
            null
    }

    fun setDarkModeFlag(isEnabled: Boolean, context: Context) {
        val sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("darkMode", isEnabled)
        editor.apply()
    }
}
