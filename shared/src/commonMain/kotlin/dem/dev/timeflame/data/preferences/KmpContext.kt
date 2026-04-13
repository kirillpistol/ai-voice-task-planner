package dem.dev.timeflame.data.preferences

expect class KmpContext

expect fun KmpContext.putInt(key: String, value: Int)

expect fun KmpContext.getInt(key: String, default: Int): Int

expect fun KmpContext.putString(key: String, value: String)

expect fun KmpContext.getString(key: String): String?

expect fun KmpContext.putBool(key: String, value: Boolean)

expect fun KmpContext.getBool(key: String, default: Boolean): Boolean

expect fun KmpContext.removeValue(key: String)