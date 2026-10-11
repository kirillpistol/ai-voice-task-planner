package dem.dev.timeflame.util.state

enum class ResultType {
    SUCCESS, FAILURE
}

data class Loading(val messageCode: Int? = null, val silentMode: Boolean = false /* Silent mode: don't show loading dialog */)
data class Result(val resultType: ResultType, val messageCode: Int? = null)

sealed interface ScreenState {
    data class Loading(val messageCode: Int? = null, val silentMode: Boolean = false /* Silent mode: don't show loading dialog */): ScreenState
    data class Result(val resultType: ResultType, val messageCode: Int? = null): ScreenState
    data object Idle: ScreenState
}