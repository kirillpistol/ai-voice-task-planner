package dem.dev.timeflame.util

import dem.dev.timeflame.feature.calendar.model.CalendarDay
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.fromDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDaysExtTest {
    @Test
    fun testCalendarDaysSort() {
        val calendarDays = listOf(1, 2, 25, 5, 15, 6, 3, 8, 9).map { day ->
            CalendarDay(requireNotNull(KDateTime.fromDate(2024, 11, day)), mutableListOf())
        }
        assertEquals(
            listOf(1, 2, 3, 5, 6, 8, 9, 15, 25),
            calendarDays.sorted().map { it.day.dayOfMonth }
        )
    }
}
