package dem.dev.timeflame.util.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// PISTOL GENESIS: neutral graphite palette for readable light and dark modes.
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF222222),
    onPrimary = Color.White,
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF181818),
    surface = Color.White,
    onSurface = Color(0xFF181818),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF555555),
    error = AppColors.red,
    outline = AppColors.gray,
    onTertiary = Color(0xFF181818)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEAEAEA),
    onPrimary = Color(0xFF171717),
    secondary = Color(0xFF444444),
    onSecondary = Color.White,
    background = Color(0xFF141414),
    onBackground = Color(0xFFF7F7F7),
    surface = Color(0xFF202020),
    onSurface = Color(0xFFF7F7F7),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFB9B9B9),
    error = AppColors.red,
    outline = Color(0xFF777777),
    onTertiary = Color.White
)
