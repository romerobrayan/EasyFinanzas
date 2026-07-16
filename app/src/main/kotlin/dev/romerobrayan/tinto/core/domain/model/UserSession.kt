package dev.romerobrayan.tinto.core.domain.model

/**
 * Who is using the app right now. Repositories route on this: [SignedIn]
 * reads/writes the user's cloud ledger, [Demo] serves the bundled sample data.
 */
sealed interface UserSession {

    /** Auth state not yet known (app just launched). */
    data object Loading : UserSession

    data object SignedOut : UserSession

    /** Exploring with sample data; nothing is persisted. */
    data object Demo : UserSession

    data class SignedIn(val user: AuthUser) : UserSession
}
