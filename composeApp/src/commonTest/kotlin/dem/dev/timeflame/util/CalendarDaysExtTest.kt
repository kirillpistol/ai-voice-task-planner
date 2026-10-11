package dem.dev.timeflame.util

import dem.dev.timeflame.feature.calendar.model.CalendarDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDaysExtTest {
    @Test
    fun testCalendarDaysSort() {
        val calendarDays = listOf(
            CalendarDay(LocalDate(2024, 11, 1), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 2), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 25), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 5), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 15), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 6), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 3), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 8), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 9), mutableListOf()),
        )
        val sortedDays = listOf(
            CalendarDay(LocalDate(2024, 11, 1), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 2), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 3), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 5), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 6), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 8), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 9), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 15), mutableListOf()),
            CalendarDay(LocalDate(2024, 11, 25), mutableListOf())
        )
        assertEquals(sortedDays, calendarDays.sorted())
    }
}