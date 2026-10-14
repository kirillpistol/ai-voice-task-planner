package dem.dev.timeflame.data.repository

import com.google.android.gms.tasks.Task as GoogleTask
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** User-scoped SQL copies for GENESIS. Never creates/edit calendar tasks. */
@Serializable
data class GenesisPolicyConfig(
    @SerialName("policy_url") val policyUrl: String? = null,
    @SerialName("policy_version") val policyVersion: String = "",
    @SerialName("import_available") val importAvailable: Boolean = false
)

@Serializable
data class GenesisSqlTask(
    @SerialName("source_task_id") val sourceTaskId: String,
    val title: String,
    val description: String = "",
    @SerialName("due_at") val dueAt: String? = null,
    val completed: Boolean = false
)

@Serializable
data class GenesisRecommendation(
    val id: String,
    val summary: String,
    @SerialName("generated_at") val generatedAt: String
)

@Serializable
data class GenesisImportResult(
    val imported: Int,
    val total: Int
)

@Serializable
private data class GenesisImportConsent(
    @SerialName("consent_to_personal_analysis") val consent: Boolean,
    @SerialName("policy_version") val policyVersion: String
)

@Serializable
data class GenesisMemoryPolicy(
    @SerialName("policy_url") val policyUrl: String? = null,
    @SerialName("policy_version") val policyVersion: String = "",
    val available: Boolean = false
)

@Serializable
data class GenesisMemoryRule(val id: String, val kind: String, val text: String)

@Serializable
data class GenesisMemoryResponse(
    val status: String,
    val text: String,
    val rules: List<GenesisMemoryRule> = emptyList(),
    @SerialName("consent_accepted") val consentAccepted: Boolean = false,
    val source: String? = null,
    @SerialName("connection_resource") val connectionResource: String? = null,
    @SerialName("security_system") val securitySystem: String? = null
)

@Serializable
private data class MemoryComment(val sector: String, val kind: String, val comment: String, val language: String)

@Serializable
private data class MemoryConfirmation(
    val sector: String, val kind: String, val comment: String, val language: String,
    @SerialName("policy_version") val policyVersion: String,
    @SerialName("consent_to_store") val consentToStore: Boolean
)

class GenesisWorkspaceClient(
    private val httpClient: HttpClient,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val baseUrl = "https://api.pistolgenesis.ru/api/v2/genesis"

    private suspend fun bearer(): String {
        val user = auth.currentUser ?: error("Not signed in")
        val token = user.getIdToken(false).awaitToken().token ?: error("Missing Firebase token")
        return "Bearer $token"
    }

    private fun checkStatus(code: Int, expected: Int) {
        if (code != expected) error("GENESIS API unavailable ($code)")
    }

    suspend fun memoryPolicy(): GenesisMemoryPolicy {
        val response = httpClient.get("$baseUrl/memory/config")
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun memory(sector: String, language: String): GenesisMemoryResponse {
        val authorization = bearer()
        val response = httpClient.get("$baseUrl/memory/$sector?language=$language") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun previewComment(sector: String, kind: String, comment: String, language: String): GenesisMemoryResponse {
        val authorization = bearer()
        val response = httpClient.post("$baseUrl/memory/preview") {
            header(HttpHeaders.Authorization, authorization)
            contentType(ContentType.Application.Json)
            setBody(MemoryComment(sector, kind, comment, language))
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun confirmComment(sector: String, kind: String, comment: String, language: String,
                               policyVersion: String, consent: Boolean): GenesisMemoryResponse {
        val authorization = bearer()
        val response = httpClient.post("$baseUrl/memory/confirm") {
            header(HttpHeaders.Authorization, authorization)
            contentType(ContentType.Application.Json)
            setBody(MemoryConfirmation(sector, kind, comment, language, policyVersion, consent))
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun forgetRule(sector: String, ruleId: String) {
        val authorization = bearer()
        val response = httpClient.delete("$baseUrl/memory/$sector/$ruleId") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 204)
    }

    suspend fun config(): GenesisPolicyConfig {
        val response = httpClient.get("$baseUrl/config")
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun tasks(): List<GenesisSqlTask> {
        val authorization = bearer()
        val response = httpClient.get("$baseUrl/tasks") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun recommendations(): List<GenesisRecommendation> {
        val authorization = bearer()
        val response = httpClient.get("$baseUrl/recommendations") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun importCalendarTasks(currentPolicyVersion: String): GenesisImportResult {
        require(currentPolicyVersion.isNotBlank()) { "Policy not published" }
        val authorization = bearer()
        val response = httpClient.post("$baseUrl/tasks/import") {
            header(HttpHeaders.Authorization, authorization)
            contentType(ContentType.Application.Json)
            setBody(GenesisImportConsent(consent = true, policyVersion = currentPolicyVersion))
        }
        checkStatus(response.status.value, 200)
        return response.body()
    }

    suspend fun clearTasks() {
        val authorization = bearer()
        val response = httpClient.delete("$baseUrl/tasks") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 200)
    }

    suspend fun deleteTask(sourceId: String) {
        require(sourceId.isNotBlank())
        val authorization = bearer()
        val response = httpClient.delete("$baseUrl/tasks/${sourceId}") {
            header(HttpHeaders.Authorization, authorization)
        }
        checkStatus(response.status.value, 204)
    }
}

private suspend fun <T> GoogleTask<T>.awaitToken(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { response ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (response.isSuccessful) continuation.resume(response.result)
            else continuation.resumeWithException(
                response.exception ?: IllegalStateException("Firebase token failure")
            )
        }
    }

