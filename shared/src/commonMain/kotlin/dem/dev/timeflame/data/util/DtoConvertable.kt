package dem.dev.timeflame.data.util

interface DtoConvertable<T> {
    fun convertToModel(): T
}