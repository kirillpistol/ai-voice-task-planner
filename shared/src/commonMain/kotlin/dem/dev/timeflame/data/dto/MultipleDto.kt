package dem.dev.timeflame.data.dto

import dem.dev.timeflame.data.util.DtoConvertable
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class MultipleDto<T: DtoConvertable<*>>(
    private val v: List<T>
): DtoConvertable<List<*>> {
    override fun convertToModel(): List<*> {
        return this.v.mapNotNull { it.convertToModel() }
    }
}