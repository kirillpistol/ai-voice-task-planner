package dem.dev.timeflame.feature.calendar.model

import dem.dev.timeflame.util.datetime.KDateTime
import kotlinx.datetime.LocalDateTime

data class Week(
    val start: KDateTime,
    val end: KDateTime,
    val days: List<CalendarDay>
)