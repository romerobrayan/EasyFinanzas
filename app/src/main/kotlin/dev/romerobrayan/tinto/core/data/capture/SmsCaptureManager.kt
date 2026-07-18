package dev.romerobrayan.tinto.core.data.capture

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.repository.SmsCapture
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The capture opt-in facade behind the [SmsCapture] domain contract.
 * Enabling (after the runtime permissions were granted) flips the gate for
 * the live receiver and kicks the one-time backfill on the app scope so it
 * survives leaving the screen.
 */
@Singleton
class SmsCaptureManager @Inject constructor(
    private val settings: CaptureSettings,
    private val backfill: SmsBackfill,
    private val applicationScope: CoroutineScope,
    private val analytics: TintoAnalytics,
) : SmsCapture {

    override val enabled: StateFlow<Boolean> = settings.smsCaptureEnabled

    override fun onPermissionsGranted() {
        analytics.logCapturePermissionGranted()
        settings.setSmsCaptureEnabled(true)
        applicationScope.launch { backfill.runOnce() }
    }

    override fun disable() {
        settings.setSmsCaptureEnabled(false)
    }
}
