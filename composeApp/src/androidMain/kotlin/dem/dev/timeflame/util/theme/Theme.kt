package dem.dev.timeflame.util.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(
    darkTheme: Boolean = DarkModeHelper.isDarkModeEnabled() ?: isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if(darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        typography = Typography(),
        content = content,
        colorScheme = colorScheme
    )
}