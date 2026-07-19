package dev.romerobrayan.tinto.core.data.capture.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.capture.CaptureSettings
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.repository.NotificationCapture
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The notification-capture opt-in facade behind the [NotificationCapture]
 * domain contract. Unlike SMS there is no backfill to kick — the listener
 * only sees notifications posted from now on — so enabling is just flipping
 * the gate the service checks on every post. Access can be revoked at any
 * time in system settings; [refreshAccess] keeps the UI honest about it.
 */
@Singleton
class NotificationCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: CaptureSettings,
    private val analytics: TintoAnalytics,
) : NotificationCapture {

    override val enabled: StateFlow<Boolean> = settings.notificationCaptureEnabled

    private val _accessGranted = MutableStateFlow(readAccess())

    override val accessGranted: StateFlow<Boolean> = _accessGranted.asStateFlow()

    override fun refreshAccess() {
        _accessGranted.value = readAccess()
    }

    override fun onAccessGranted() {
        analytics.logCapturePermissionGranted(CaptureChannel.NOTIFICATION.name)
        settings.setNotificationCaptureEnabled(true)
        refreshAccess()
    }

    override fun disable() {
        settings.setNotificationCaptureEnabled(false)
    }

    private fun readAccess(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
}
