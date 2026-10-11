package dem.dev.timeflame.data.preferences

import android.app.Application

actual typealias KmpContext = Application

const val SP_NAME = "timeflame"

actual fun KmpContext.putInt(key: String, value: Int) {
    getSpEditor().putInt(key, value).apply()
}

actual fun KmpContext.getInt(key: String, default: Int): Int = getSp().getInt(key, default)

actual fun KmpContext.putString(key: String, value: String) {
    getSpEditor().putString(key, value).apply()
}

actual fun KmpContext.getString(key: String): String? = getSp().getString(key, null)

actual fun KmpContext.putBool(key: String, value: Boolean) {
    getSpEditor().putBoolean(key, value).apply()
}

actual fun KmpContext.getBool(key: String, default: Boolean): Boolean = getSp().getBoolean(key, default)

private fun KmpContext.getSp() = getSharedPreferences(SP_NAME, 0)
private fun KmpContext.getSpEditor() = getSp().edit()

actual fun KmpContext.removeValue(key: String) {
    getSpEditor().remove(key).apply()
}