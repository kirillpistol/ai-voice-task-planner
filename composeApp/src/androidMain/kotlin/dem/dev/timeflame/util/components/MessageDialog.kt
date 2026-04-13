package dem.dev.timeflame.util.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class MessageType {
    SUCCESS, WARNING, ERROR
}

data class MessageDialog(
    val type: MessageType,
    val message: String
)

@Composable
fun MessageDialog(
    dialog: MessageDialog? = null,
    onDismiss: () -> Unit
) {
    dialog?.let {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card (
                elevation = CardDefaults.elevatedCardElevation(3.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(vertical = 20.dp, horizontal = 30.dp)
                ) {
                    val messageIcon = when(dialog.type) {
                        MessageType.SUCCESS -> Icons.Outlined.Check
                        MessageType.WARNING -> Icons.Outlined.Warning
                        MessageType.ERROR -> Icons.Outlined.Error
                    }

                    Image(
                        imageVector = messageIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(16.dp)
                    )
                    Text(
                        text = dialog.message,
                        modifier = Modifier
                            .padding(top = 15.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}