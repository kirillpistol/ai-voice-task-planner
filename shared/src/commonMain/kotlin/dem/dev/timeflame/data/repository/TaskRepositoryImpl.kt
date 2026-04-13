package dem.dev.timeflame.data.repository

import dem.dev.timeflame.data.dto.CreateTaskRequestDto
import dem.dev.timeflame.data.dto.EmptyResponse
import dem.dev.timeflame.data.dto.MultipleDto
import dem.dev.timeflame.data.dto.Response
import dem.dev.timeflame.data.dto.TaskDto
import dem.dev.timeflame.data.util.toDto
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.TaskRepository
import dem.dev.timeflame.util.restApiConfig.BackendEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepositoryImpl(
    private val httpClient: HttpClient
): TaskRepository {
    override suspend fun getTasksByUserIdInDiapason(
        userId: String,
        start: Long,
        end: Long
    ): Result<List<Task>> {
        try {
            val response = httpClient.get(BackendEndpoints.Task.getByUserIdInDiapason) {
                parameter("userId", userId)
                parameter("start", start)
                parameter("end", end)
            }.body<Response<MultipleDto<TaskDto>>>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun createTask(createTaskRequestDto: CreateTaskRequestDto): Result<Task> {
        try {
            val response = httpClient.post(BackendEndpoints.Task.create) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(createTaskRequestDto)
            }.body<Response<TaskDto>>()
            return response.convertToModel<Task>()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun deleteTask(taskId: Int): Result<Unit> {
        try {
            val response = httpClient.delete(BackendEndpoints.Task.delete) {
                parameter("taskId", taskId)
            }.body<EmptyResponse>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        try {
            val response = httpClient.post(BackendEndpoints.Task.edit) {
                contentType(ContentType.Application.Json)
                setBody(task.toDto())
            }.body<EmptyResponse>()
            return response.convertToModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result(ResponseCode.badRequest, null)
        }
    }
}