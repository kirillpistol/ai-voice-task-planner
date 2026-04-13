package dem.dev.timeflame.util.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LoadingDialog(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    message: String? = null,
    onDismiss: () -> Unit = {}
) {
    if (isLoading) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(17.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        .padding(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(130.dp)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        message?.let { loadingMsg ->
                            Text(
                                text = loadingMsg,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }

                }
            }
        }
    }
}