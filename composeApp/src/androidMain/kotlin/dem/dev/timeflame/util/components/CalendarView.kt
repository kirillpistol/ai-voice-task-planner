package dem.dev.timeflame.util.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.feature.calendar.model.CalendarDay
import dem.dev.timeflame.feature.main.MainScreenEvent
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.fromDate
import dem.dev.timeflame.util.sorted
import dem.dev.timeflame.util.theme.AppTheme
import kotlinx.datetime.LocalDate

@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    calendarDays: List<CalendarDay> = emptyList(),
    selectedDayIndex: Int,
    onDayClicked: (Int) -> Unit = {}
) {
    val daysShortNames = listOf(
        Pair(1, stringResource(R.string.monday_short)),
        Pair(2, stringResource(R.string.tuesday_short)),
        Pair(3, stringResource(R.string.wednesday_short)),
        Pair(4, stringResource(R.string.thursday_short)),
        Pair(5, stringResource(R.string.friday_short)),
        Pair(6, stringResource(R.string.saturday_short)),
        Pair(7, stringResource(R.string.sunday_short))
    )
    LazyRow(
        modifier = modifier
    ) {
        items(daysShortNames) { weekDay ->
            Column {
                CalendarItem(
                    text = weekDay.second,
                    textColor = MaterialTheme.colorScheme.outline
                )

                LazyColumn {
                    itemsIndexed(calendarDays.filter { it.day.dayOfWeek == weekDay.first }.sorted()) { i, day ->
                        Column {
                            if (i == 0 && day.day.after(calendarDays.sorted()[0].day)
                                && day.day.dayOfWeek < calendarDays.sorted()[0].day.dayOfWeek) {
                                // adding empty cell before
                                EmptyCell()
                            }

                            CalendarItem(
                                isSelected = (calendarDays[selectedDayIndex].day == day.day),
                                onClick = {
                                    val originalIndex = calendarDays.indexOf(day)
                                    if (originalIndex != -1)
                                        onDayClicked(originalIndex)
                                },
                                tasksAmount = day.tasks.size,
                                text = day.day.dayOfMonth.toString()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarItem(
    modifier: Modifier = Modifier,
    text: String = "",
    textColor: Color = MaterialTheme.colorScheme.onTertiary,
    isSelected: Boolean = false,
    tasksAmount: Int = 0,
    onClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .size(45.dp)
            .clickable (
                onClick = {
                    onClick()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else textColor
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TasksAmountIndicator(
                modifier = Modifier.padding(top = 2.dp),
                tasksAmount = tasksAmount,
                isSelected = isSelected
            )
        }
    }
}

@Composable
private fun EmptyCell(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(45.dp)
    )
}

@Composable
private fun TasksAmountIndicator(
    modifier: Modifier = Modifier,
    tasksAmount: Int = 0,
    isSelected: Boolean = false,
) {
    val indicatorColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val amount = if (tasksAmount > 3) 3 else tasksAmount
        for (i in 1..amount) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(indicatorColor, CircleShape)
            )
            if (i != amount) {
                Spacer(modifier = Modifier.width(3.dp))
            }
        }
    }

}

@Preview
@Composable
fun CalendarPreview() {
    val task = Task(
        id = 1,
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