package dem.dev.timeflame.domain.model

data class Task(
    val id: Int,
    val text: String,
    val userId: String,
    val timestamp: Long,
    val completed: Boolean,
    val notificationSent: Boolean
)
