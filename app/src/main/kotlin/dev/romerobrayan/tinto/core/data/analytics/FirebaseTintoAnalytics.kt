package dev.romerobrayan.tinto.core.data.analytics

import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTintoAnalytics @Inject constructor() : TintoAnalytics {

    private val analytics get() = Firebase.analytics
    private val crashlytics get() = Firebase.crashlytics

    override fun setUser(userId: String?) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId.orEmpty())
    }

    override fun logScreenView(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName),
        )
    }

    override fun logLogin(method: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.LOGIN,
            bundleOf(FirebaseAnalytics.Param.METHOD to method),
        )
    }

    override fun logDemoMode() {
        analytics.logEvent("demo_mode", null)
    }

    override fun logSignOut() {
        analytics.logEvent("sign_out", null)
    }

    override fun logAddTransaction(type: String, method: String) {
        logTransactionEvent("add_transaction", type, method)
    }

    override fun logEditTransaction(type: String, method: String) {
        logTransactionEvent("edit_transaction", type, method)
    }

    override fun logDeleteTransaction(type: String, method: String) {
        logTransactionEvent("delete_transaction", type, method)
    }

    override fun logAddCard() {
        analytics.logEvent("add_card", null)
    }

    override fun logDeleteCard() {
        analytics.logEvent("delete_card", null)
    }

    override fun logAddReminder(recurrence: String) {
        analytics.logEvent("add_reminder", bundleOf("recurrence" to recurrence))
    }

    override fun logReminderPaid(recurrence: String) {
        analytics.logEvent("reminder_paid", bundleOf("recurrence" to recurrence))
    }

    override fun logRecurringRuleCreated(frequency: String) {
        analytics.logEvent("recurring_rule_created", bundleOf("frequency" to frequency))
    }

    override fun logReminderNotificationShown(recurrence: String) {
        analytics.logEvent("reminder_notification_shown", bundleOf("recurrence" to recurrence))
    }

    override fun logCapturePermissionGranted(channel: String) {
        analytics.logEvent("capture_permission_granted", bundleOf("channel" to channel))
    }

    override fun logCaptureDetected(channel: String, issuer: String) {
        analytics.logEvent(
            "capture_detected",
            bundleOf(
                "channel" to channel,
                "issuer" to issuer,
            ),
        )
    }

    override fun logPendingConfirmed(count: Int) {
        analytics.logEvent("pending_confirmed", bundleOf("count" to count.toLong()))
    }

    override fun logPendingDiscarded(count: Int) {
        analytics.logEvent("pending_discarded", bundleOf("count" to count.toLong()))
    }

    override fun logPendingDuplicateShown() {
        analytics.logEvent("pending_duplicate_shown", null)
    }

    override fun recordError(error: Throwable) {
        crashlytics.recordException(error)
    }

    private fun logTransactionEvent(event: String, type: String, method: String) {
        analytics.logEvent(
            event,
            bundleOf(
                "transaction_type" to type,
                "payment_method" to method,
            ),
        )
    }
}
