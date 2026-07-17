package dev.romerobrayan.tinto.core.data.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification capture seam (Nu). Deliberately NOT registered in the manifest
 * and never bound this sprint — the class exists so the fast-follow only has
 * to fill in the mapping and the manifest entry, not reshape the pipeline.
 */
class TintoNotificationListenerService : NotificationListenerService() {

    // TODO(sprint-4): make this an @AndroidEntryPoint, inject CapturePipeline,
    //  register the service in the manifest behind the system-settings grant,
    //  and map posted notifications from allow-listed packages to
    //  RawCapture(channel = NOTIFICATION) with receivedAt = sbn.postTime —
    //  Nu bodies carry relative dates, so the posted timestamp is the date
    //  source (TASK_SPRINT_3_CAPTURE.md, Nu field reference).
    override fun onNotificationPosted(sbn: StatusBarNotification) = Unit
}
