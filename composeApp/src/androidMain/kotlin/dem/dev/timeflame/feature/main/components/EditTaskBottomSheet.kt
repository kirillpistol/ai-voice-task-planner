package dem.dev.timeflame.feature.main.components

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.AppLanguage
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.util.datetime.Format
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.convertToZone
import dem.dev.timeflame.util.datetime.copy
import dem.dev.timeflame.util.datetime.timestamp
import dem.dev.timeflame.util.datetime.toLocalDateTime
import dem.dev.timeflame.util.theme.AppColors
import dem.dev.timeflame.util.theme.AppTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    modifier: Modifier = Modifier,
    task: Task, // task to edit
    onDismissRequest: () -> Unit,
    onTaskSave: (Task) -> Unit
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        EditTaskBottomSheetContent(
            task = task,
            onDismissRequest = onDismissRequest,
            onTaskSave = onTaskSave
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskBottomSheetContent(
    task: Task, // task to edit
    onDismissRequest: () -> Unit,
    onTaskSave: (Task) -> Unit
) {
    // initial timestamp of task (in DB) is in UTC and we save it in UTC, but display in the local timezone
    var taskDate by remember { mutableStateOf(task.timestamp.toLocalDateTime() ) }
    var taskTitle by remember { mutableStateOf(task.text) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = taskDate.copy(hour = 0, minute = 0, second = 0, nanosecond = 0).timestamp() /* Start of day */)
    var showDatePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(taskDate.hour, taskDate.minute)
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val onSaveBtnClicked: () -> Unit = {
        if (taskTitle.isEmpty())
            error = context.getString(R.string.need_to_set_task_title)
        else {
            // initial timestamp of task (in DB) is in UTC and we save it in UTC, but display in the local timezone
            val updatedTask = task.copy(text = taskTitle, timestamp = taskDate.timestamp())
            onTaskSave(updatedTask)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val selectedDate = (datePickerState.selectedDateMillis?:task.timestamp).toLocalDateTime()
                        taskDate = taskDate.copy(year = selectedDate.year, month = selectedDate.month, dayOfMonth = selectedDate.dayOfMonth)
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(
                modifier = Modifier.padding(10.dp),
                state = datePickerState
            )
        }
    }
    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        taskDate = taskDate.copy(hour = timePickerState.hour, minute = timePickerState.minute)
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                    }
                ) { Text("Cancel") }
            }
        ) {
            TimePicker(
                modifier = Modifier.padding(15.dp),
                state = timePickerState
            )
        }
    }

    Column(
        modifier = Modifier.padding(15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.edit_task),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Image(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.clickable { onDismissRequest() }
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            Text(
                text = taskDate.format(Format.dateFormat),
                modifier = Modifier
                    .background(AppColors.orangeLight, RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .clickable { showDatePicker = true },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = taskDate.format(Format.timeFormat),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(AppColors.orangeLight, RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .clickable { showTimePicker = true },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        error?.let {
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                    .padding(5.dp)
            ) {
                Image(
                    imageVector = Icons.Default.Error,
                    contentDescription = null
                )
                Text(
                    text = it,
                    modifier = Modifier
                        .padding(start = 10.dp)
                )
            }

        }

        OutlinedTextField(
            value = taskTitle,
            onValueChange = { taskTitle = it },
            placeholder = {
                Text(stringResource(R.string.task_text))
            },
            modifier = Modifier
                .padding(top = 15.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                textColor = MaterialTheme.colorScheme.onTertiary
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onSaveBtnClicked() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    disabledBackgroundColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .padding(top = 25.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview
@Composable
fun EditTaskBottomSheetPreview() {
    val task = Task(
        id = 1,
        text = "New task to edit",
        userId = "",
        timestamp = 1235345545,
        completed = false,
        notificationSent = false
    )
    AppTheme {
        EditTaskBottomSheetContent(
            task = task,
            onDismissRequest = {},
            onTaskSave = {}
        )
    }
}