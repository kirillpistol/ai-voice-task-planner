package dem.dev.timeflame.feature.calendar.usecase

import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.util.datetime.UtcTimeZone

class LoadCalendarForMonthUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(userId: String, month: Month): Result<List<Task>> {
        // Month boundaries already represent local calendar instants as UTC epoch milliseconds.
        // Do not subtract the device offset a second time.
        return taskRepository.getTasksByUserIdInDiapason(userId, month.start.timestamp(), month.end.timestamp())
    }
}