package dem.dev.timeflame.domain.manager

import dem.dev.timeflame.domain.model.LocalUser

interface LocalAuthManager {
    fun getCurrentUser(): LocalUser?
    fun authorize(user: LocalUser)
    fun signOut()
}