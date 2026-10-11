package dem.dev.timeflame.feature.calendar.model

import dem.dev.timeflame.domain.model.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Regression: recording a task in the evening must NOT put it on tomorrow.
 * All timestamps below are real UTC instants produced from explicit local dates.
 */
class MonthDateMappingTest {
    private fun task(id: String, day: Int, hour: Int, zone: TimeZone, month: Int = 10): Task {
        val timestamp = LocalDateTime(2026, month, day, hour, 45)
            .toInstant(zone).toEpochMilliseconds()
        return Task(
            id = id,
            text = "Task $id",
            userId = "test-user",
            timestamp = timestamp,
            completed = false,
            notificationSent = false
        )
    }

    @Test
    fun eveningTasksRemainOnSelectedDayInPositiveUtcZone() {
        val zone = TimeZone.of("Asia/Yekaterinburg")
        val month = assertNotNull(Month.byNumberAndYear(10, 2026))
        val fourTasks = listOf(
            task("first", 22, 19, zone),
            task("second", 22, 21, zone),
            task("third", 22, 22, zone),
            task("fourth", 22, 23, zone)
        )
        month.sortTasksByDays(fourTasks, zone)

        assertEquals(4, month.days.first { it.day.dayOfMonth == 22 }.tasks.size)
        assertEquals(0, month.days.first { it.day.dayOfMonth == 23 }.tasks.size)
    }

    @Test
    fun fourthAndFifthTasksNeverSpillIntoTomorrow() {
        val zone = TimeZone.of("Europe/Berlin")
        val month = assertNotNull(Month.byNumberAndYear(10, 2026))
        val tasks = (1..5).map { number -> task("task-$number", 10, 22, zone) }
        month.sortTasksByDays(tasks, zone)

        assertEquals(5, month.days.first { it.day.dayOfMonth == 10 }.tasks.size)
        assertEquals(0, month.days.first { it.day.dayOfMonth == 11 }.tasks.size)

        // Repeated backend refresh does not append duplicates.
        month.sortTasksByDays(tasks, zone)
        assertEquals(5, month.days.first { it.day.dayOfMonth == 10 }.tasks.size)
    }

    @Test
    fun tasksAtMonthBoundaryStayOnTheirLocalDates() {
        val zone = TimeZone.of("Europe/Berlin")
        val october = assertNotNull(Month.byNumberAndYear(10, 2026))
        val tasks = listOf(
            task("oct-last", 31, 23, zone),
            task("nov-first", 1, 0, zone, month = 11)
        )
        october.sortTasksByDays(tasks, zone)

        assertEquals(listOf("oct-last"), october.days.first { it.day.dayOfMonth == 31 }.tasks.map { it.id })
        assertEquals(1, october.days.sumOf { it.tasks.size })
    }

    @Test
    fun negativeUtcOffsetAlsoPreservesLocalDay() {
        val zone = TimeZone.of("America/Los_Angeles")
        val month = assertNotNull(Month.byNumberAndYear(10, 2026))
        month.sortTasksByDays(listOf(task("late", 10, 23, zone)), zone)
        assertEquals(listOf("late"), month.days.first { it.day.dayOfMonth == 10 }.tasks.map { it.id })
    }
}
