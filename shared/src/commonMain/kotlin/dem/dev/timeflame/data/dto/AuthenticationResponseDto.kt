package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.AuthenticationResponse
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResponseDto(
    val token: String,
    val id: String
): DtoConvertable<AuthenticationResponse> {
    override fun convertToModel(): AuthenticationResponse {
        return AuthenticationResponse(
            token = this.token,
            id = this.id
        )
    }
}
