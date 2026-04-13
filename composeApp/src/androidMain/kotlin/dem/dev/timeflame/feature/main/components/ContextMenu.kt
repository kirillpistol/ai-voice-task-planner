package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp

enum class ContextMenuOption {
    ChangeStatus, Delete, Edit
}

@Composable
fun ContextMenu(
    modifier: Modifier,
    onOptionSelected: (ContextMenuOption) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContextMenuItem(
            icon = {
                Image(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            },
            onClick = {
                onOptionSelected(ContextMenuOption.ChangeStatus)
            }
        )
        ContextMenuItem(
            icon = {
                Image(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            },
            onClick = {
                onOptionSelected(ContextMenuOption.Delete)
            }
        )
        ContextMenuItem(
            icon = {
                Image(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            },
            onClick = {
                onOptionSelected(ContextMenuOption.Edit)
            }
        )
    }
}