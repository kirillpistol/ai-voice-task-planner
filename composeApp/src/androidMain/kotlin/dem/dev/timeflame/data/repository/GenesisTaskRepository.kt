package dem.dev.timeflame.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task as GoogleTask
import com.google.firebase.auth.FirebaseAuth
import dem.dev.timeflame.data.dto.CreateTaskRequestDto
import dem.dev.timeflame.domain.model.ResponseCode
import dem.dev.timeflame.domain.model.Result
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.TaskRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Android API v2 integration; Firebase owns identity and refreshes ID tokens. */
class GenesisTaskRepository(
    private val httpClient: HttpClient,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : TaskRepository {

    private val baseUrl = "https://api.pistolgenesis.ru/api/v2/tasks"

    private suspend fun authHeader(): String {
        val firebaseUser = firebaseAuth.currentUser ?: error("Not signed in")
        val token = firebaseUser.getIdToken(false).awaitFirebaseTask().token
            ?: error("Missing Firebase ID token")
        return "Bearer $token"
    }

    override suspend fun getTasksByUserIdInDiapason(
        userId: String,
        start: Long,
        end: Long
    ): Result<List<Task>> = try {
        val response = httpClient.get(baseUrl) {
            header(HttpHeaders.Authorization, authHeader())
            url { parameters.append("limit", "100") }
        }
        if (response.status.value != 200) {
            Result(response.status.value, null)
        } else {
            // v2 currently exposes at most 100 tasks; future versions need cursor pagination.
            val tasks = response.body<List<ApiTask>>()
                .map { it.toTask(userId) }
                .filter { it.timestamp in start..end }
            Result(ResponseCode.ok, tasks)
        }
    } catch (error: Exception) {
        Log.w("GenesisTasks", "Task loading failed: ${error.javaClass.simpleName}")
        Result(ResponseCode.badRequest, null)
    }

    override suspend fun createTask(createTaskRequestDto: CreateTaskRequestDto): Result<Task> = try {
        val dueAt = LocalDateTime.parse(
            createTaskRequestDto.currentDateTimeStr,
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        ).atZone(ZoneId.systemDefault()).toInstant().toString()
        val response = httpClient.post(baseUrl) {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(ApiTaskInput(title = createTaskRequestDto.taskText, dueAt = dueAt))
        }
        if (response.status.value != 201) {
            Result(response.status.value, null)
        } else {
            Result(ResponseCode.created, response.body<ApiTask>().toTask(createTaskRequestDto.userId))
        }
    } catch (error: Exception) {
        Log.w("GenesisTasks", "Task creation failed: ${error.javaClass.simpleName}")
        Result(ResponseCode.badRequest, null)
    }

    override suspend fun updateTask(task: Task): Result<Unit> = try {
        val response = httpClient.put("$baseUrl/${task.id}") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(
                ApiTaskInput(
                    title = task.text,
                    dueAt = Instant.ofEpochMilli(task.timestamp).toString(),
                    completed = task.completed
                )
            )
        }
        Result(if (response.status.value == 200) ResponseCode.ok else response.status.value, null)
    } catch (error: Exception) {
        Log.w("GenesisTasks", "Task update failed: ${error.javaClass.simpleName}")
        Result(ResponseCode.badRequest, null)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = try {
        val response = httpClient.delete("$baseUrl/$taskId") {
            header(HttpHeaders.Authorization, authHeader())
        }
        Result(if (response.status.value == 204) ResponseCode.updated else response.status.value, null)
    } catch (error: Exception) {
        Log.w("GenesisTasks", "Task deletion failed: ${error.javaClass.simpleName}")
        Result(ResponseCode.badRequest, null)
    }
}

@Serializable
private data class ApiTaskInput(
    val title: String,
    val description: String = "",
    @SerialName("due_at") val dueAt: String? = null,
    val completed: Boolean = false
)

@Serializable
private data class ApiTask(
    val id: String,
    val title: String,
    val description: String = "",
    @SerialName("due_at") val dueAt: String? = null,
    val completed: Boolean = false
) {
    fun toTask(userId: String) = Task(
        id = id,
        text = title,
        userId = userId,
        timestamp = dueAt?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
            ?: System.currentTimeMillis(),
        completed = completed,
        notificationSent = false
    )
}

private suspend fun <T> GoogleTask<T>.awaitFirebaseTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { completed ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (completed.isSuccessful) {
                continuation.resume(completed.result)
            } else {
                continuation.resumeWithException(
                    completed.exception ?: IllegalStateException("Firebase request failed")
                )
            }
        }
    }
