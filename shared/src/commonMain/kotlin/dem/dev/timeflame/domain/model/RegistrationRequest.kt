package dem.dev.timeflame.domain.model

data class RegistrationRequest(
    val name: String,
    val email: String,
    val password: String
)
