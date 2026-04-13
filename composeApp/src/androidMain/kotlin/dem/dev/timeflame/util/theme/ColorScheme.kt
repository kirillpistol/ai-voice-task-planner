package dem.dev.timeflame.util.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = AppColors.orange,
    onPrimary = AppColors.white,

    secondary = AppColors.darksecondary,
    onSecondary = AppColors.white,

    background = AppColors.lightGray,
    onBackground = AppColors.white,

    error = AppColors.red,
    outline = AppColors.gray,

    onSurface = AppColors.gray2, // for hint text color

    onTertiary = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = AppColors.orange,
    onPrimary = AppColors.white,

    secondary = AppColors.darksecondary,
    onSecondary = AppColors.white,

    background = AppColors.dark,
    onBackground = AppColors.darksecondary,

    error = AppColors.red,
    outline = AppColors.gray,

    onSurface = AppColors.gray2, // for hint text color

    onTertiary = Color.White
)