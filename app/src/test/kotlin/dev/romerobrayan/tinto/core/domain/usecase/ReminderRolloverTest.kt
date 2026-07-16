package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the recurrence rollover: NONE completes; WEEKLY/MONTHLY/YEARLY advance
 * the due date one period and stay unpaid. Month and year arithmetic clamp
 * the day-of-month (kotlinx-datetime behavior we deliberately accept).
 */
class ReminderRolloverTest {

    private fun reminder(
        dueDate: LocalDate,
        recurrence: Recurrence,
        isPaid: Boolean = false,
    ) = Reminder(
        id = "rem-1",
        title = "Arriendo",
        amount = Money.ofPesos(950_000),
        dueDate = dueDate,
        recurrence = recurrence,
        isPaid = isPaid,
    )

    @Test
    fun `one-off reminder becomes paid and keeps its due date`() {
        val paid = reminder(LocalDate(2026, 7, 15), Recurrence.NONE).markAsPaid()

        assertTrue(paid.isPaid)
        assertEquals(LocalDate(2026, 7, 15), paid.dueDate)
    }

    @Test
    fun `weekly reminder advances seven days and stays unpaid`() {
        val rolled = reminder(LocalDate(2026, 7, 15), Recurrence.WEEKLY).markAsPaid()

        assertFalse(rolled.isPaid)
        assertEquals(LocalDate(2026, 7, 22), rolled.dueDate)
    }

    @Test
    fun `weekly rollover crosses a month boundary`() {
        val rolled = reminder(LocalDate(2026, 7, 29), Recurrence.WEEKLY).markAsPaid()

        assertEquals(LocalDate(2026, 8, 5), rolled.dueDate)
    }

    @Test
    fun `monthly reminder advances one month and stays unpaid`() {
        val rolled = reminder(LocalDate(2026, 7, 15), Recurrence.MONTHLY).markAsPaid()

        assertFalse(rolled.isPaid)
        assertEquals(LocalDate(2026, 8, 15), rolled.dueDate)
    }

    @Test
    fun `monthly rollover clamps january 31 to february 28`() {
        val rolled = reminder(LocalDate(2026, 1, 31), Recurrence.MONTHLY).markAsPaid()

        assertEquals(LocalDate(2026, 2, 28), rolled.dueDate)
    }

    @Test
    fun `monthly rollover clamps to february 29 on leap years`() {
        val rolled = reminder(LocalDate(2028, 1, 31), Recurrence.MONTHLY).markAsPaid()

        assertEquals(LocalDate(2028, 2, 29), rolled.dueDate)
    }

    @Test
    fun `monthly rollover clamps august 31 to september 30`() {
        val rolled = reminder(LocalDate(2026, 8, 31), Recurrence.MONTHLY).markAsPaid()

        assertEquals(LocalDate(2026, 9, 30), rolled.dueDate)
    }

    @Test
    fun `monthly rollover crosses a year boundary`() {
        val rolled = reminder(LocalDate(2026, 12, 15), Recurrence.MONTHLY).markAsPaid()

        assertEquals(LocalDate(2027, 1, 15), rolled.dueDate)
    }

    @Test
    fun `yearly reminder advances one year and stays unpaid`() {
        val rolled = reminder(LocalDate(2026, 7, 15), Recurrence.YEARLY).markAsPaid()

        assertFalse(rolled.isPaid)
        assertEquals(LocalDate(2027, 7, 15), rolled.dueDate)
    }

    @Test
    fun `yearly rollover clamps february 29 to february 28`() {
        val rolled = reminder(LocalDate(2028, 2, 29), Recurrence.YEARLY).markAsPaid()

        assertEquals(LocalDate(2029, 2, 28), rolled.dueDate)
    }
}
