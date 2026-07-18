package dev.romerobrayan.tinto.core.data.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Scaffolded seam for the Nu push-notification capture. Deliberately NOT
 * registered in the manifest and never started — SMS is the only live
 * capture source this sprint. The parser and staging store are already
 * channel-agnostic ([dev.romerobrayan.tinto.core.domain.model.CaptureChannel]),
 * so wiring this up is: register the service, map a posted notification to
 * RawCapture(sender = packageName, receivedAt = postTime), and add the Nu
 * rule set to IssuerRules.
 */
// TODO(sprint-4): implement Nu notification capture behind this seam.
class TintoNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // TODO(sprint-4): normalize to RawCapture and feed CaptureProcessor.
    }
}
