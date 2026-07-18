package dev.romerobrayan.tinto.core.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.domain.model.Reminder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

/**
 * AlarmManager wrapper for reminder alerts. One pending intent per reminder
 * id (request code from the id hash) so re-scheduling and cancelling are
 * idempotent. Inexact by design — `setAndAllowWhileIdle` tolerates minutes
 * of drift and avoids the Android 12+ exact-alarm special access.
 *
 * The scheduled id set is persisted so a reconcile after process death can
 * still cancel alarms whose reminders were deleted meanwhile.
 */
@Singleton
class ReminderAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    /** Aligns the OS alarms with the desired set: schedules these, cancels the rest. */
    fun reconcile(upcoming: List<Pair<Reminder, Instant>>) {
        val previousIds = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()
        val currentIds = upcoming.mapTo(mutableSetOf()) { (reminder, _) -> reminder.id }

        (previousIds - currentIds).forEach(::cancel)
        upcoming.forEach { (reminder, triggerAt) ->
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilliseconds(),
                alarmIntent(reminder.id) {
                    putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
                    putExtra(ReminderAlarmReceiver.EXTRA_AMOUNT_CENTS, reminder.amount?.cents ?: NO_AMOUNT)
                    putExtra(ReminderAlarmReceiver.EXTRA_RECURRENCE, reminder.recurrence.name)
                },
            )
        }

        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, currentIds).apply()
    }

    private fun cancel(reminderId: String) {
        alarmManager.cancel(alarmIntent(reminderId) {})
    }

    /** Matching for cancel is component + action + request code — extras don't count. */
    private fun alarmIntent(reminderId: String, configure: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ReminderAlarmReceiver.ACTION_REMINDER_DUE)
            .apply(configure)
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val PREFS_NAME = "reminder_alarms"
        const val KEY_SCHEDULED_IDS = "scheduled_ids"
        const val NO_AMOUNT = -1L
    }
}
