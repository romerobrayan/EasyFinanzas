package dev.romerobrayan.tinto.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for the SMS capture opt-in, so features never touch the
 * data-layer capture glue. Enabling assumes the runtime permissions were
 * granted by the UI first; it flips the live-receive gate and runs the
 * one-time bounded backfill.
 */
interface SmsCapture {

    val enabled: StateFlow<Boolean>

    /** Call only after RECEIVE_SMS + READ_SMS were granted. */
    fun onPermissionsGranted()

    /** Stops live capture; already-staged items stay in the inbox. */
    fun disable()
}
