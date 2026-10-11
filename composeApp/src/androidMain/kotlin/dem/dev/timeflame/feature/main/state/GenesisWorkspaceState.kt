package dem.dev.timeflame.feature.main.state

import dem.dev.timeflame.data.repository.GenesisPolicyConfig
import dem.dev.timeflame.data.repository.GenesisRecommendation
import dem.dev.timeflame.data.repository.GenesisSqlTask

/** The SQL workspace is independent of calendar tasks in Firestore. */
data class GenesisWorkspaceState(
    val policy: GenesisPolicyConfig? = null,
    val tasks: List<GenesisSqlTask> = emptyList(),
    val recommendations: List<GenesisRecommendation> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val notice: GenesisWorkspaceNotice = GenesisWorkspaceNotice.NONE
)

enum class GenesisWorkspaceNotice { NONE, CONNECTION_CHECKED, IMPORTED, CLEARED, TASK_DELETED }
