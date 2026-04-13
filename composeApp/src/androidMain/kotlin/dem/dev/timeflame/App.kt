package dem.dev.timeflame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.navigation.MainNavHost
import dem.dev.timeflame.util.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    modifier: Modifier = Modifier
) {
    AppTheme {
        MainNavHost(
            modifier = modifier
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(top = 35.dp)
        )
    }
}