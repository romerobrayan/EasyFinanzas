package dev.romerobrayan.tinto.core.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Money
import javax.inject.Inject

/**
 * Fires when a reminder's alarm goes off: shows the local notification from
 * the intent extras (no async repository read at alarm time). Analytics
 * carries the recurrence only — never the title or amount.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var analytics: TintoAnalytics

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_DUE) return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val amount = intent.getLongExtra(EXTRA_AMOUNT_CENTS, -1L)
            .takeIf { it >= 0 }
            ?.let(::Money)

        ReminderNotifications.show(context, reminderId, title, amount)
        analytics.logReminderNotificationShown(
            intent.getStringExtra(EXTRA_RECURRENCE) ?: "NONE",
        )
    }

    companion object {
        const val ACTION_REMINDER_DUE = "dev.romerobrayan.tinto.action.REMINDER_DUE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AMOUNT_CENTS = "amount_cents"
        const val EXTRA_RECURRENCE = "recurrence"
    }
}
