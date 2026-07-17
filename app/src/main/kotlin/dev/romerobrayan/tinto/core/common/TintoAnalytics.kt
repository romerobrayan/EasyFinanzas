package dev.romerobrayan.tinto.core.common

/**
 * Product analytics + crash-reporting identity, behind an interface so no
 * Firebase types leak into features. Events carry no amounts, merchants or
 * any other personal finance data — names and coarse enums only.
 */
interface TintoAnalytics {

    /** Ties analytics + crash reports to the signed-in uid; null clears it. */
    fun setUser(userId: String?)

    fun logScreenView(screenName: String)

    /** [method] e.g. "google". */
    fun logLogin(method: String)

    fun logDemoMode()

    fun logSignOut()

    /** Coarse shape of the movement only: EXPENSE/INCOME + CARD/CASH. */
    fun logAddTransaction(type: String, method: String)

    /** Coarse shape of the movement only: EXPENSE/INCOME + CARD/CASH. */
    fun logEditTransaction(type: String, method: String)

    /** Coarse shape of the movement only: EXPENSE/INCOME + CARD/CASH. */
    fun logDeleteTransaction(type: String, method: String)

    /** No bank names or digits — the event alone. */
    fun logAddCard()

    /** No bank names or digits — the event alone. */
    fun logDeleteCard()

    /** [recurrence] is the coarse enum name only — never titles or amounts. */
    fun logAddReminder(recurrence: String)

    /** [recurrence] is the coarse enum name only — never titles or amounts. */
    fun logReminderPaid(recurrence: String)

    /** Capture opt-in completed with the SMS permissions granted. */
    fun logCapturePermissionGranted()

    /**
     * A capture was staged for review. Coarse [channel] (SMS/NOTIFICATION) and
     * [issuer] (bank name) only — never amounts, merchants, senders or raw text.
     */
    fun logCaptureDetected(channel: String, issuer: String)

    /** A pending capture was confirmed into the ledger. The event alone. */
    fun logPendingConfirmed()

    /** A pending capture was discarded. The event alone. */
    fun logPendingDiscarded()

    /** The review sheet opened flagged as a likely duplicate. The event alone. */
    fun logPendingDuplicateShown()

    /** Non-fatal error worth seeing in Crashlytics (e.g. a sync listener failure). */
    fun recordError(error: Throwable)
}
