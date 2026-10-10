package dem.dev.timeflame.util.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.feature.calendar.model.CalendarDay
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.fromDate
import dem.dev.timeflame.util.theme.AppTheme

/**
 * Stable seven-column calendar for both MONTH and WEEK modes.
 * Indexes returned to the caller always refer to the original calendarDays list.
 */
@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay> = emptyList(),
    selectedDayIndex: Int,
    onDayClicked: (Int) -> Unit = {}
) {
    val weekdayLabels = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    // Keep original indexes while displaying days chronologically.
    val sortedDays = calendarDays.withIndex().sortedBy { it.value.day.timestamp() }
    val paddingBefore = sortedDays.firstOrNull()?.value?.day?.dayOfWeek?.minus(1) ?: 0
    val cells: List<IndexedValue<CalendarDay>?> =
        List(paddingBefore) { null } + sortedDays
    val selectedDay = calendarDays.getOrNull(selectedDayIndex)?.day

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) { column ->
                    val date = week.getOrNull(column)
                    Box(
                        modifier = Modifier.weight(1f).height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val day = date.value.day
                            val isSelected = selectedDay != null &&
                                selectedDay.year == day.year &&
                                selectedDay.monthNumber == day.monthNumber &&
                                selectedDay.dayOfMonth == day.dayOfMonth
                            CalendarDateCell(
                                dayNumber = day.dayOfMonth,
                                taskCount = date.value.tasks.size,
                                selected = isSelected,
                                onClick = { onDayClicked(date.index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDateCell(
    dayNumber: Int,
    taskCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primary
        else androidx.compose.ui.graphics.Color.Transparent
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .widthIn(max = 44.dp)
            .fillMaxWidth()
            .height(44.dp)
            .background(background, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = foreground
        )
        if (taskCount > 0) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(taskCount.coerceAtMost(3)) {
                    Box(
                        Modifier.size(4.dp).background(foreground, CircleShape)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CalendarPreview() {
    val task = Task(
        id = "preview",
        text = "Task text",
        userId = "userId",
        timestamp = 0,
        completed = false,
        notificationSent = false
    )
    AppTheme {
        Calendar(
            calendarDays = listOf(
                CalendarDay(KDateTime.fromDate(2024, 11, 1)!!, mutableListOf(task, task)),
                CalendarDay(KDateTime.fromDate(2024, 11, 2)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 3)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 4)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 5)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 6)!!, mutableListOf(task, task, task, task, task, task)),
                CalendarDay(KDateTime.fromDate(2024, 11, 7)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 8)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 9)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 10)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 11)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 12)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 13)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 14)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 15)!!, mutableListOf(task, task, task, task, task, task)),
                CalendarDay(KDateTime.fromDate(2024, 11, 16)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 17)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 18)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 19)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 20)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 21)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 22)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 23)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 24)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 25)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 26)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 27)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 28)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 29)!!, mutableListOf()),
                CalendarDay(KDateTime.fromDate(2024, 11, 30)!!, mutableListOf()),
            ),
            selectedDayIndex = 1
        )
    }
}