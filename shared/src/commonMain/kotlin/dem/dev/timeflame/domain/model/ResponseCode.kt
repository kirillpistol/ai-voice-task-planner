package dem.dev.timeflame.domain.model

object ResponseCode {
    const val ok = 200
    const val created = 201
    const val accepted = 202
    const val updated = 204

    const val badRequest = 400
    const val userAlreadyExists = 470
    const val notFound = 481
    const val wrongCredentials = 482
}