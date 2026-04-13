package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import dem.dev.timeflame.domain.model.Result
import kotlinx.serialization.Serializable

@Serializable
data class Response<T: DtoConvertable<*> /* Data dto*/>(
    val code: Int,
    val data: T? = null
) {
    inline fun <reified K /* Domain model class that dto is being converted to */> convertToModel(): Result<K> {
        val convertedData = if (data?.convertToModel() is K) data?.convertToModel() as? K else throw RuntimeException("Failed to correctly convert the dto to model")

        return Result(
            code = this.code,
            data = convertedData
        )
    }
}


@Serializable
data class EmptyResponse(
    val code: Int
) {
    inline fun convertToModel(): Result<Unit> {
        return Result(code = this.code)
    }
}