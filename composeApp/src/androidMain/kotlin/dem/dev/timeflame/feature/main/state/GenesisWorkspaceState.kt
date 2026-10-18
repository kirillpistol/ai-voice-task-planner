package dem.dev.timeflame.feature.main.state

import dem.dev.timeflame.data.repository.GenesisAiAnalysis
import dem.dev.timeflame.data.repository.GenesisPolicyConfig
import dem.dev.timeflame.data.repository.GenesisRecommendation
import dem.dev.timeflame.data.repository.GenesisSqlTask
import dem.dev.timeflame.data.repository.GenesisMemoryPolicy
import dem.dev.timeflame.data.repository.GenesisMemoryRule
import dem.dev.timeflame.data.repository.GenesisCoreStatus

/** The SQL workspace is independent of calendar tasks in Firestore. */
data class GenesisWorkspaceState(
    val policy: GenesisPolicyConfig? = null,
    val acceptedPolicyVersion: String? = null,
    val tasks: List<GenesisSqlTask> = emptyList(),
    val recommendations: List<GenesisRecommendation> = emptyList(),
    val moduleOutputs: Map<String, GenesisModuleOutput> = emptyMap(),
    val coreStatus: GenesisCoreStatus? = null,
    val coreLoading: Boolean = false,
    val coreFailed: Boolean = false,
    val memoryPolicy: GenesisMemoryPolicy? = null,
    val memoryConsentAccepted: Boolean = false,
    val memoryBusySector: String? = null,
    val memoryFailedSector: String? = null,
    val memoryRules: Map<String, List<GenesisMemoryRule>> = emptyMap(),
    val commentDrafts: Map<String, String> = emptyMap(),
    val aiResults: Map<String, GenesisAiAnalysis> = emptyMap(),
    val aiBusySector: String? = null,
    val aiFailedSector: String? = null,
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

