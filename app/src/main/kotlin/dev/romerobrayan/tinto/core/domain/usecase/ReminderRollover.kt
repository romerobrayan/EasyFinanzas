package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * Recurrence-aware "mark as paid". One-off reminders complete and move to the
 * paid section; recurring ones roll the due date forward one period and stay
 * unpaid, so the next occurrence is already scheduled. Month/year arithmetic
 * clamps the day-of-month (Jan 31 + 1 month = Feb 28/29) — pinned by
 * ReminderRolloverTest.
 */
fun Reminder.markAsPaid(): Reminder = when (recurrence) {
    Recurrence.NONE -> copy(isPaid = true)
    Recurrence.WEEKLY -> copy(dueDate = dueDate.plus(1, DateTimeUnit.WEEK), isPaid = false)
    Recurrence.MONTHLY -> copy(dueDate = dueDate.plus(1, DateTimeUnit.MONTH), isPaid = false)
    Recurrence.YEARLY -> copy(dueDate = dueDate.plus(1, DateTimeUnit.YEAR), isPaid = false)
}
