package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.AuthUser
import dev.romerobrayan.tinto.core.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    /** Current session; starts as [UserSession.Loading] and settles right after startup. */
    val session: StateFlow<UserSession>

    /**
     * Nickname of the device-only profile if one was ever created — kept after
     * leaving it so the gate can offer it back. Null when there is none.
     */
    val localProfileName: StateFlow<String?>

    /**
     * Exchanges a Google ID token (obtained by the UI via Credential Manager,
     * which needs an Activity context) for a Firebase session.
     * Throws when the credential is rejected or there is no connectivity.
     */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /** Switches to [UserSession.Demo]; in-memory sample data, not persisted. */
    fun enterDemoMode()

    /**
     * Switches to [UserSession.Local] under [displayName] — a device-only
     * ledger, no account. Re-entering with an existing profile keeps whatever
     * is already stored on the device; only the nickname is overwritten.
     */
    fun continueLocally(displayName: String)

    /**
     * Ends the session (also leaves demo or the local profile) and returns to
     * the login screen. Local data stays on the device — leaving is not
     * deleting; the nickname is kept so the gate can prefill it.
     */
    fun signOut()
}
