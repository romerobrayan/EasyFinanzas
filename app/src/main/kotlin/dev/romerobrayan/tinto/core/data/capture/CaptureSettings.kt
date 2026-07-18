package dev.romerobrayan.tinto.core.data.capture

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-local capture preferences. The SMS opt-in is the gate for the whole
 * pipeline: nothing is read — live or backfill — until the user enables it
 * explicitly (and the runtime permissions are granted on top).
 */
@Singleton
class CaptureSettings @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _smsCaptureEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMS_ENABLED, false))

    /** Whether the user opted in to SMS capture. */
    val smsCaptureEnabled: StateFlow<Boolean> = _smsCaptureEnabled.asStateFlow()

    fun setSmsCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_ENABLED, enabled).apply()
        _smsCaptureEnabled.value = enabled
    }

    /** True once the one-time inbox backfill ran; re-runs are no-ops anyway (dedup). */
    var backfillDone: Boolean
        get() = prefs.getBoolean(KEY_BACKFILL_DONE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BACKFILL_DONE, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "capture_settings"
        const val KEY_SMS_ENABLED = "sms_capture_enabled"
        const val KEY_BACKFILL_DONE = "sms_backfill_done"
    }
}
