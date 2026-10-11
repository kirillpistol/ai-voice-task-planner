package dem.dev.timeflame.feature.task.usecase

import dem.dev.timeflame.data.dto.CreateTaskRequestDto
import dem.dev.timeflame.data.dto.TaskDto
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.TimeZone
import dem.dev.timeflame.util.datetime.getTimeZoneOffset
import dem.dev.timeflame.util.datetime.localTimeZone
import dem.dev.timeflame.util.datetime.now
import kotlinx.datetime.LocalDateTime

class CreateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(userId: String, taskText: String, dateTime: String): Result<Task> {
        val timeZoneOffset = localTimeZone.getTimeZoneOffset() // getting local time zone offset in hours
        val createTaskRequestDto = CreateTaskRequestDto(
            userId = userId,
            taskText = taskText,
            currentDateTimeStr = dateTime,
            timeZoneOffset = timeZoneOffset
        )

        return taskRepository.createTask(createTaskRequestDto)
    }
}