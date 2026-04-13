package dem.dev.timeflame.feature.main.components

import android.graphics.Paint.Align
import android.view.RoundedCorner
import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.AppLanguage
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.util.datetime.Format
import dem.dev.timeflame.util.datetime.convertToZone
import dem.dev.timeflame.util.datetime.toLocalDateTime
import dem.dev.timeflame.util.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlin.math.roundToInt

@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    task: Task,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onStatusChangeClicked: () -> Unit
) {
    var contextMenuWidth by remember {
        mutableFloatStateOf(0f)
    }
    val offset = remember {
        androidx.compose.animation.core.Animatable(initialValue = 0f)
    }
    val scope = rememberCoroutineScope()
    val completenessIndicatorColor = if (task.completed) colorResource(R.color.green) else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        ContextMenu(
            modifier = Modifier
                .fillMaxHeight()
                .onSizeChanged {
                    contextMenuWidth = it.width.toFloat()
                }
                .background(completenessIndicatorColor, RoundedCornerShape(topStart = 15.dp, bottomStart = 15.dp)),
            onOptionSelected = {
                when(it) {
                    ContextMenuOption.ChangeStatus -> onStatusChangeClicked()
                    ContextMenuOption.Edit -> onEditClicked()
                    ContextMenuOption.Delete -> onDeleteClicked()
                }
                CoroutineScope(scope.coroutineContext).launch {
                    offset.animateTo(0f)
                }
            }
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(contextMenuWidth) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (offset.value + dragAmount)
                                    .coerceIn(0f, contextMenuWidth)
                                offset.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            when {
                                offset.value >= contextMenuWidth / 2f -> {
                                    scope.launch {
                                        offset.animateTo(contextMenuWidth)
                                    }
                                }
                                else -> {
                                    scope.launch {
                                        offset.animateTo(0f)
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            TaskItemContent(
                task = task
            )
        }
    }
}

@Composable
private fun TaskItemContent(
    modifier: Modifier = Modifier,
    task: Task
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val completenessIndicatorColor = if (task.completed) colorResource(R.color.green) else MaterialTheme.colorScheme.primary
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(completenessIndicatorColor, RoundedCornerShape(topStart = 15.dp, bottomStart = 15.dp)))

        Column(
            modifier = Modifier.padding(start = 15.dp)
        ) {
            Text(
                text = task.timestamp.toLocalDateTime().format(Format.dateTimeFormat1(AppLanguage.RUS)),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = task.text,
                modifier = Modifier.padding(top = 5.dp, end = 20.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun TaskItemPreview() {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TaskItem(
                task = Task(
                    0,
                    "Task text asdcasdc asdc asdc asdc asdc asdc asdca sdc asdc asdc asdc adascas asdca",
                    "",
                    123412345656,
                    false,
                    false
                ),
                onEditClicked = {},
                onStatusChangeClicked = {},
                onDeleteClicked = {}
            )
        }
    }
}