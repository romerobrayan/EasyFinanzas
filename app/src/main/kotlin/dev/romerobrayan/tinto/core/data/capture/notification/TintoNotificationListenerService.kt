package dev.romerobrayan.tinto.core.data.capture.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import dev.romerobrayan.tinto.core.data.capture.CaptureProcessor
import dev.romerobrayan.tinto.core.data.capture.CaptureSettings
import dev.romerobrayan.tinto.core.data.capture.parser.IssuerRules
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * Live Nu capture: a notification posted by an allow-listed package →
 * RawCapture → CaptureProcessor. Gated on the explicit opt-in in Perfil;
 * anything not on the package allowlist is ignored silently, before any
 * parsing. There is no history backfill — the listener only sees what is
 * posted while access is granted ("desde ahora"). Re-emissions of the same
 * notification collapse at staging via the entity dedup key (time-bucketed
 * for this channel), so a re-post never resurrects a confirmed/discarded
 * item — same discipline as the SMS backfill.
 */
@AndroidEntryPoint
class TintoNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var processor: CaptureProcessor

    @Inject lateinit var settings: CaptureSettings

    @Inject lateinit var applicationScope: CoroutineScope

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!settings.notificationCaptureEnabled.value) return
        if (sbn.packageName !in IssuerRules.nu.senders) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        // Prefer the expanded text — some Nu posts carry the amount only there.
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()
        val body = listOf(title, text).filter(String::isNotBlank).joinToString(" ")
        if (body.isBlank()) return

        applicationScope.launch {
            processor.process(
                RawCapture(
                    sender = sbn.packageName,
                    body = body,
                    receivedAt = Instant.fromEpochMilliseconds(sbn.postTime),
                    channel = CaptureChannel.NOTIFICATION,
                ),
            )
        }
    }
}
