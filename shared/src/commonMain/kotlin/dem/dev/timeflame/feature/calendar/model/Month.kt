package dem.dev.timeflame.feature.calendar.model

import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.util.datetime.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

data class Month(
    val start: KDateTime,
    val end: KDateTime,
    val days: MutableList<CalendarDay>
) {
    val number: Int
        get() = start.monthNumber

    val year: Int
        get() = start.year

    fun sortTasksByDays(tasks: List<Task>): Month {
        tasks.forEach { task ->
            val taskDate = KDateTime.fromUtcTimestamp(task.timestamp)
            val calendarDay = this.days.find { it.day.formatToString(KDateTimeFormat.dateFormat1) == taskDate.formatToString(KDateTimeFormat.dateFormat1) } ?: return@forEach /* If we didn't find calendar day for this task */

            calendarDay.tasks.add(task)
        }
        return this.copy()
    }

    fun getWeekForDay(calendarDay: CalendarDay): Week? {
        val day = calendarDay.day
        val weekDates = day.currentWeek() ?: return null

        val days = this.days.filter {
            it.day.between(weekDates.first, weekDates.second)
        }
        return Week(
            start = weekDates.first,
            end = weekDates.second,
            days = days
        )
    }
    fun next(): Month? {
        val nextMonthNumber = if (this.start.monthNumber == 12) 1 else this.start.monthNumber+1

        // getting first day of the next month in order to get borders of the month (first and last day)
        val firstDayOfNextMonth = KDateTime.fromDate(
            year = if (nextMonthNumber == 1) this.start.year+1 else this.start.year,
            month = nextMonthNumber,
            day = 1
        )

        // getting month "borders"
        val nextMonthFirstAndLastDays = firstDayOfNextMonth?.startOfDay()?.currentMonth()

        val firstDay = nextMonthFirstAndLastDays?.first ?: return null
        val endDay = nextMonthFirstAndLastDays.second
        var currentDay = firstDay
        val calendarDays = mutableListOf<CalendarDay>()

        while (endDay.after(currentDay)) {
            calendarDays.add(CalendarDay(currentDay, mutableListOf()))
            currentDay = currentDay.plusDays(1) ?: return null
        }
        return Month(
            start = nextMonthFirstAndLastDays.first,
            end = nextMonthFirstAndLastDays.second,
            days = calendarDays
        )
    }
    fun previous(): Month? {
        val previousMonth = if (this.start.monthNumber == 1) 12 else this.start.monthNumber-1

        // getting first day of the next month in order to get borders of the month (first and last day)
        val firstDayOfNextMonth = KDateTime.fromDate(
            year = if (previousMonth == 12) this.start.year-1 else this.start.year,
            month = previousMonth,
            day = 1
        ) ?: return null
        // getting month "borders"
        val nextMonthFirstAndLastDays = firstDayOfNextMonth.currentMonth() ?: return null

        val firstDay = nextMonthFirstAndLastDays.first
        val endDay = nextMonthFirstAndLastDays.second
        var currentDay = firstDay
        val calendarDays = mutableListOf<CalendarDay>()

        while (endDay.after(currentDay)) {
            calendarDays.add(CalendarDay(currentDay, mutableListOf()))
            currentDay = currentDay.plusDays(1)?: return null
        }
        return Month(
            start = nextMonthFirstAndLastDays.first,
            end = nextMonthFirstAndLastDays.second,
            days = calendarDays
        )
    }

    companion object {
        fun current(): Month? {
            val month = KDateTime.now().currentMonth() ?: return null

            val firstDay = month.first
            val endDay = month.second
            var currentDay = firstDay
            val calendarDays = mutableListOf<CalendarDay>()

            while (endDay.after(currentDay) || endDay == currentDay) {
                calendarDays.add(CalendarDay(currentDay, mutableListOf()))
                currentDay = currentDay.plusDays(1) ?: return null
            }
            return Month(
                start = month.first,
                end = month.second,
                days = calendarDays
            )
        }
        fun byNumberAndYear(monthNumber: Int, year: Int): Month? {
            // check if month number is valid
            if (monthNumber !in 1..12) return null

            // getting first day of the next month in order to get borders of the month (first and last day)
            val firstDayOfNextMonth = KDateTime.fromDate(
                year = year,
                month = monthNumber,
                day = 1
            )

            // getting month "borders"
            val monthFirstAndLastDays = firstDayOfNextMonth?.startOfDay()?.currentMonth()

            val firstDay = monthFirstAndLastDays?.first ?: return null
            val endDay = monthFirstAndLastDays.second
            var currentDay = firstDay
            val calendarDays = mutableListOf<CalendarDay>()

            while (endDay.after(currentDay)) {
                calendarDays.add(CalendarDay(currentDay, mutableListOf()))
                currentDay = currentDay.plusDays(1) ?: return null
            }
            return Month(
                start = monthFirstAndLastDays.first,
                end = monthFirstAndLastDays.second,
                days = calendarDays
            )
        }
    }
}