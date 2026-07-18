package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Reminder
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** Hour of day a date-only reminder alerts at (no dueTime picked). */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime(8, 0)

/**
 * When this reminder's local notification should fire: `dueDate` at
 * `dueTime`, or at [DEFAULT_REMINDER_TIME] when the reminder is date-only.
 * Null when nothing should be scheduled — the reminder is paid or the
 * trigger is already in the past (never alert late).
 */
fun Reminder.nextTriggerInstant(now: Instant, timeZone: TimeZone): Instant? {
    if (isPaid) return null
    val trigger = dueDate.atTime(dueTime ?: DEFAULT_REMINDER_TIME).toInstant(timeZone)
    return trigger.takeIf { it > now }
}
