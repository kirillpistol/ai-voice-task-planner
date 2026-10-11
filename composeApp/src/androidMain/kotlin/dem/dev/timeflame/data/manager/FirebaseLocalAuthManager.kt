package dem.dev.timeflame.data.manager

import com.google.firebase.auth.FirebaseAuth
import dem.dev.timeflame.data.preferences.KmpPreference
import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.domain.model.LocalUser

/** Prevent legacy v1 credentials from bypassing Firebase sign-in. */
class FirebaseLocalAuthManager(
    preferences: KmpPreference,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : LocalAuthManager {

    private val local = LocalAuthManagerImpl(preferences)

    override fun getCurrentUser(): LocalUser? {
        val current = auth.currentUser ?: return null
        return local.getCurrentUser()?.takeIf { it.id == current.uid }
    }

    override fun authorize(user: LocalUser) = local.authorize(user)

    override fun signOut() {
        auth.signOut()
        local.signOut()
    }
}
