package dev.romerobrayan.tinto.core.data.capture

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.domain.repository.CaptureSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SharedPreferences-backed opt-in state (a single flag doesn't warrant
 * DataStore). Enabling kicks the bounded SMS backfill right here — the data
 * layer owns the capture pipeline, so the UI only ever flips the switch.
 */
@Singleton
class SharedPreferencesCaptureSettings @Inject constructor(
    @ApplicationContext context: Context,
    private val smsCaptureSource: SmsCaptureSource,
    private val applicationScope: CoroutineScope,
) : CaptureSettingsRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val enabled = MutableStateFlow(preferences.getBoolean(KEY_SMS_ENABLED, false))

    override val smsCaptureEnabled: StateFlow<Boolean> = enabled.asStateFlow()

    override fun setSmsCaptureEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_SMS_ENABLED, enabled) }
        this.enabled.value = enabled
        if (enabled) {
            // Idempotent: the seen-key index keeps re-runs from re-staging.
            applicationScope.launch { smsCaptureSource.backfill() }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "capture_settings"
        const val KEY_SMS_ENABLED = "sms_capture_enabled"
    }
}
