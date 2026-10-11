package dem.dev.timeflame.data.manager

import dem.dev.timeflame.data.preferences.KEY_ID
import dem.dev.timeflame.data.preferences.KEY_TOKEN
import dem.dev.timeflame.data.preferences.KmpPreference
import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.model.LocalUser

class LocalAuthManagerImpl(
    private val kmpPreference: KmpPreference
): LocalAuthManager {
    override fun getCurrentUser(): LocalUser? {
        val token = kmpPreference.getString(KEY_TOKEN)?:return null
        val id = kmpPreference.getString(KEY_ID)?:return null

        return LocalUser(token, id)
    }

    override fun authorize(user: LocalUser) {
        kmpPreference.put(KEY_TOKEN, user.token)
        kmpPreference.put(KEY_ID, user.id)
    }

    override fun signOut() {
        kmpPreference.remove(KEY_TOKEN)
        kmpPreference.remove(KEY_ID)
    }
}