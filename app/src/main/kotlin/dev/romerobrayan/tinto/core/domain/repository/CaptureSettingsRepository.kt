package dev.romerobrayan.tinto.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The automatic-capture opt-in. Everything in the capture pipeline is gated
 * on this: no SMS is read — live or backfill — while the user hasn't
 * explicitly enabled it.
 */
interface CaptureSettingsRepository {

    val smsCaptureEnabled: StateFlow<Boolean>

    /**
     * Persists the opt-in. Enabling also kicks the bounded, idempotent SMS
     * inbox backfill in the data layer (already-seen messages never stage
     * twice), so callers only ever flip this switch.
     */
    fun setSmsCaptureEnabled(enabled: Boolean)
}
