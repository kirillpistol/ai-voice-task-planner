package dem.dev.timeflame.domain.model

data class AuthenticationRequest(
    val email: String,
    val password: String
)
