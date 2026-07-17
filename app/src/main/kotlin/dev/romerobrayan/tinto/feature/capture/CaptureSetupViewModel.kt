package dev.romerobrayan.tinto.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.repository.CaptureSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CaptureSetupUiState(
    val smsEnabled: Boolean = false,
)

/**
 * The capture opt-in. The screen owns the runtime permission request (it
 * needs the Activity); this ViewModel only ever flips the persisted setting —
 * enabling triggers the bounded SMS backfill inside the data layer.
 */
@HiltViewModel
class CaptureSetupViewModel @Inject constructor(
    private val captureSettings: CaptureSettingsRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    val uiState: StateFlow<CaptureSetupUiState> = captureSettings.smsCaptureEnabled
        .map { CaptureSetupUiState(smsEnabled = it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CaptureSetupUiState(smsEnabled = captureSettings.smsCaptureEnabled.value),
        )

    /** Both SMS permissions granted → opt in (also kicks the 90-day backfill). */
    fun onPermissionsGranted() {
        analytics.logCapturePermissionGranted()
        captureSettings.setSmsCaptureEnabled(true)
    }

    fun onDisable() {
        captureSettings.setSmsCaptureEnabled(false)
    }
}
