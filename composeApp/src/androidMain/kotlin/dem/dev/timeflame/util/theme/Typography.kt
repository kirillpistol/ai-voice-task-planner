package dem.dev.timeflame.util.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dem.dev.timeflame.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle

@Composable
fun InterFontFamily() = FontFamily(
    Font(R.font.inter, FontWeight.Light),
    Font(R.font.inter, weight = FontWeight.Normal),
    Font(R.font.inter, weight = FontWeight.Medium),
    Font(R.font.inter, weight = FontWeight.SemiBold),
    Font(R.font.inter, weight = FontWeight.Bold),
    Font(R.font.inter_italic, style = FontStyle.Italic)
)

@Composable
fun Typography() = Typography().run {
    val interFont = InterFontFamily()

    copy(
        bodyLarge = bodyLarge.copy(fontFamily = interFont),
        titleMedium = titleMedium.copy(fontFamily = interFont),
        displaySmall = displaySmall.copy(fontFamily = interFont)
    )
}