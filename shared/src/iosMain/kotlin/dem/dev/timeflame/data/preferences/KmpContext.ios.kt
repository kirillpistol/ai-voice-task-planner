package dem.dev.timeflame.data.preferences

import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

actual typealias KmpContext = NSObject

actual fun KmpContext.putInt(key: String, value: Int) {
    NSUserDefaults.standardUserDefaults.setInteger(value.toLong(), key)
}

actual fun KmpContext.getInt(key: String, default: Int): Int =
    NSUserDefaults.standardUserDefaults.integerForKey(key).toInt()

actual fun KmpContext.putString(key: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, key)
}

actual fun KmpContext.getString(key: String): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(key)

actual fun KmpContext.putBool(key: String, value: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(value, key)
}

actual fun KmpContext.getBool(key: String, default: Boolean): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey(key)

actual fun KmpContext.removeValue(key: String) {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
}