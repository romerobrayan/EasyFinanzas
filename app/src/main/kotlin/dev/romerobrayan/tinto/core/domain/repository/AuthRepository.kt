package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.AuthUser
import dev.romerobrayan.tinto.core.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    /** Current session; starts as [UserSession.Loading] and settles right after startup. */
    val session: StateFlow<UserSession>

    /**
     * Exchanges a Google ID token (obtained by the UI via Credential Manager,
     * which needs an Activity context) for a Firebase session.
     * Throws when the credential is rejected or there is no connectivity.
     */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /** Switches to [UserSession.Demo]; in-memory sample data, not persisted. */
    fun enterDemoMode()

    /** Ends the session (also leaves demo mode) and returns to the login screen. */
    fun signOut()
}
