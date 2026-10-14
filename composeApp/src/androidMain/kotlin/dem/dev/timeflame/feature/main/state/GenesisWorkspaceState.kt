package dem.dev.timeflame.feature.main.state

import dem.dev.timeflame.data.repository.GenesisPolicyConfig
import dem.dev.timeflame.data.repository.GenesisRecommendation
import dem.dev.timeflame.data.repository.GenesisSqlTask
import dem.dev.timeflame.data.repository.GenesisMemoryPolicy
import dem.dev.timeflame.data.repository.GenesisMemoryRule

/** The SQL workspace is independent of calendar tasks in Firestore. */
data class GenesisWorkspaceState(
    val policy: GenesisPolicyConfig? = null,
    val acceptedPolicyVersion: String? = null,
    val tasks: List<GenesisSqlTask> = emptyList(),
    val recommendations: List<GenesisRecommendation> = emptyList(),
    val moduleOutputs: Map<String, GenesisModuleOutput> = emptyMap(),
    val memoryPolicy: GenesisMemoryPolicy? = null,
    val memoryConsentAccepted: Boolean = false,
    val memoryBusySector: String? = null,
    val memoryFailedSector: String? = null,
    val memoryRules: Map<String, List<GenesisMemoryRule>> = emptyMap(),
    val commentDrafts: Map<String, String> = emptyMap(),
    val commentKinds: Map<String, String> = emptyMap(),
    val pendingRules: Map<String, GenesisPendingRule> = emptyMap(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val notice: GenesisWorkspaceNotice = GenesisWorkspaceNotice.NONE
)

data class GenesisPendingRule(val comment: String, val kind: String, val language: String)

enum class GenesisWorkspaceNotice { NONE, CONNECTION_CHECKED, IMPORTED, CLEARED, TASK_DELETED }

/** Read-only results from a sector pipeline; this model never authorizes execution. */
data class GenesisModuleOutput(
    val text: String,
    val source: String? = null,
    val connectionResource: String? = null,
    val securitySystem: String? = null
)

