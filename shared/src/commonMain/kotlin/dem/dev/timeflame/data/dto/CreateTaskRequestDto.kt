package dem.dev.timeflame.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequestDto(
    val userId: String,
    val taskText: String,
    val currentDateTimeStr: String,
    val timeZoneOffset: Int
)
