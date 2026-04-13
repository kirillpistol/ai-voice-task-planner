package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.RegistrationRequest
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequestDto(
    val name: String,
    val email: String,
    val password: String
): DtoConvertable<RegistrationRequest> {
    override fun convertToModel(): RegistrationRequest {
        return RegistrationRequest(
            name = this.name,
            email = this.email,
            password = this.password
        )
    }
}
