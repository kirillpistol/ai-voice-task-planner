package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.AuthenticationRequest
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationRequestDto(
    val email: String,
    val password: String
): DtoConvertable<AuthenticationRequest> {
    override fun convertToModel(): AuthenticationRequest {
        return AuthenticationRequest(
            email = this.email,
            password = this.password
        )
    }
}
