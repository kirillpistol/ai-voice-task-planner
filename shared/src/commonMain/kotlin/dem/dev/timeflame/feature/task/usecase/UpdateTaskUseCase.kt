package dem.dev.timeflame.feature.task.usecase

import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Unit> {
        return taskRepository.updateTask(task)
    }
}