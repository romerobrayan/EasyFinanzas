package dev.romerobrayan.tinto.core.domain.model

/**
 * Who is using the app right now. Repositories route on this: [SignedIn]
 * reads/writes the user's cloud ledger, [Local] the device-only one, [Demo]
 * serves the bundled sample data.
 */
sealed interface UserSession {

    /** Auth state not yet known (app just launched). */
    data object Loading : UserSession

    data object SignedOut : UserSession

    /** Exploring with sample data; nothing is persisted. */
    data object Demo : UserSession

    /**
     * No account: the ledger lives in this device's Room database and never
     * leaves it — no uid, no Firestore, no analytics collection. Moving the
     * data to another device is export → import, by design.
     *
     * [displayName] is the nickname the user typed at the gate; it is stored in
     * SharedPreferences and never sent anywhere.
     */
    data class Local(val displayName: String) : UserSession

    data class SignedIn(val user: AuthUser) : UserSession
}
