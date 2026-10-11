package dem.dev.timeflame.feature.calendar.model

import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.util.datetime.KDateTime
import kotlinx.datetime.LocalDate

data class CalendarDay(
    val day: KDateTime,
    val tasks: MutableList<Task>
)