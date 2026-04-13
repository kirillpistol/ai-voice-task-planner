package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.Task
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: Int,
    val text: String,
    val userId: String,
    val timestamp: Long,
    val completed: Boolean,
    val notificationSent: Boolean
): DtoConvertable<Task> {
    override fun convertToModel(): Task {
        return Task(
            id = this.id,
            text = this.text,
            userId = this.userId,
            timestamp = this.timestamp,
            completed = this.completed,
            notificationSent = this.notificationSent
        )
    }
}
