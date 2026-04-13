package dem.dev.timeflame.util

import dem.dev.timeflame.feature.calendar.model.CalendarDay

fun List<CalendarDay>.sorted(): List<CalendarDay> {
    return sortedWith { o1, o2 ->
        (o1.day.timestamp() - o2.day.timestamp()).toInt()
    }
}