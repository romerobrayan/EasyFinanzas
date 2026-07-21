package dev.romerobrayan.tinto.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for the Nu notification-capture opt-in, so features never
 * touch the data-layer capture glue. Notification access is a special access
 * the user grants in system settings (not a runtime dialog), so the effective
 * state is opt-in AND access — the UI deep-links to settings, reports the
 * grant back, and [refreshAccess] re-checks after returning (or a revocation).
 */
interface NotificationCapture {

    /** The user's explicit opt-in; survives an access revocation. */
    val enabled: StateFlow<Boolean>

    /** Whether system-level notification access is currently granted. */
    val accessGranted: StateFlow<Boolean>

    /** Re-reads the system access state (e.g. on returning to the foreground). */
    fun refreshAccess()

    /** Call only after notification access was granted in system settings. */
    fun onAccessGranted()

    /** Stops staging new notifications; already-staged items stay in the inbox. */
    fun disable()
}
