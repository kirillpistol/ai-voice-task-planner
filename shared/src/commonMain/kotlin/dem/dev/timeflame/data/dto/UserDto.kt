package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String
): DtoConvertable<User> {
    override fun convertToModel(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email
        )
    }
}
