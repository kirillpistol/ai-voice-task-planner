package dem.dev.timeflame.feature.task.usecase

import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int): Result<Unit> {
        return taskRepository.deleteTask(taskId)
    }
}