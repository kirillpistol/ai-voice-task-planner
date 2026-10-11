package dem.dev.timeflame.data.util

import dem.dev.timeflame.data.dto.AuthenticationRequestDto
import dem.dev.timeflame.data.dto.AuthenticationResponseDto
import dem.dev.timeflame.data.dto.RegistrationRequestDto
import dem.dev.timeflame.data.dto.TaskDto
import dem.dev.timeflame.data.dto.UserDto
import dem.dev.timeflame.domain.model.AuthenticationRequest
import dem.dev.timeflame.domain.model.AuthenticationResponse
import dem.dev.timeflame.domain.model.RegistrationRequest
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.model.User

fun AuthenticationRequest.toDto() = AuthenticationRequestDto(
    email = this.email,
    password = this.password
)

fun AuthenticationResponse.toDto() = AuthenticationResponseDto(
    token = this.token,
    id = this.id
)

fun RegistrationRequest.toDto() = RegistrationRequestDto(
    name = this.name,
    email = this.email,
    password = this.password
)

fun Task.toDto() = TaskDto(
    id = this.id,
    text = this.text,
    userId = this.userId,
    timestamp = this.timestamp,
    completed = this.completed,
    notificationSent = this.notificationSent
)

fun User.toDto() = UserDto(
    id = this.id,
    name = this.name,
    email = this.email
)