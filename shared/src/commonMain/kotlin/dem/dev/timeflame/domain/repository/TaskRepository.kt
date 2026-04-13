package dem.dev.timeflame.domain.repository

import dem.dev.timeflame.data.dto.CreateTaskRequestDto
import dem.dev.timeflame.data.dto.TaskDto
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task

interface TaskRepository {
    suspend fun getTasksByUserIdInDiapason(userId: String, start: Long /* Start timestamp in UTC */, end: Long /* End timestamp in UTC */): Result<List<Task>>
    suspend fun createTask(createTaskRequestDto: CreateTaskRequestDto): Result<Task>
    suspend fun deleteTask(taskId: Int): Result<Unit>
    suspend fun updateTask(task: Task): Result<Unit>
}