package dem.dev.timeflame.util

import dem.dev.timeflame.feature.calendar.model.CalendarDay

/** Compare full 64-bit timestamps; narrowing a date difference to Int overflows after ~25 days. */
fun List<CalendarDay>.sorted(): List<CalendarDay> =
    sortedWith(compareBy { it.day.timestamp() })
