package dem.dev.timeflame.domain.model

data class Result<T>(
    val code: Int,
    val data: T? = null
) {
    fun isSuccess() = when(code) {
        ResponseCode.ok -> true
        ResponseCode.created -> true
        ResponseCode.accepted -> true
        ResponseCode.updated -> true
        else -> false
    }
}
