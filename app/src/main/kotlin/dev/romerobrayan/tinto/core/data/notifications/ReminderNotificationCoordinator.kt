package dev.romerobrayan.tinto.core.data.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.usecase.nextTriggerInstant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/**
 * The scheduling seam: observes the bound (session-routed) ReminderRepository
 * from app start and reconciles alarms on every emission — unpaid future
 * reminders get scheduled, paid/deleted ones cancelled. Observing the
 * repository (not hooking CRUD calls) makes demo and signed-in behave
 * identically, covers the recurrence rollover, multi-device edits arriving
 * via Firestore sync, and signed-out (empty emission → cancel all) for free.
 */
@Singleton
class ReminderNotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val scheduler: ReminderAlarmScheduler,
    private val applicationScope: CoroutineScope,
) {

    private val started = AtomicBoolean(false)

    @Volatile
    private var latestReminders: List<Reminder> = emptyList()

    /** Called once from Application.onCreate; extra calls are no-ops. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        ReminderNotifications.ensureChannel(context)
        applicationScope.launch {
            reminderRepository.observeReminders().collect { reminders ->
                latestReminders = reminders
                reconcile(reminders)
            }
        }
    }

    /**
     * Re-registers alarms against the wall clock — after boot (alarms die on
     * reboot) and on timezone/clock changes. The ongoing collection keeps
     * feeding fresh data on top.
     */
    fun reconcileNow() {
        applicationScope.launch { reconcile(latestReminders) }
    }

    private fun reconcile(reminders: List<Reminder>) {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        scheduler.reconcile(
            reminders.mapNotNull { reminder ->
                reminder.nextTriggerInstant(now, timeZone)?.let { reminder to it }
            },
        )
    }
}
