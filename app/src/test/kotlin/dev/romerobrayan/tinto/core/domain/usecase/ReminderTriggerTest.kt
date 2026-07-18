package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the notification trigger computation: dueTime wins, date-only falls
 * back to 08:00, past-due and paid reminders never schedule, and the instant
 * is timezone-anchored (Bogotá vs UTC differ by 5 hours).
 */
class ReminderTriggerTest {

    private val bogota = TimeZone.of("America/Bogota")
    private val now = LocalDateTime(2026, 7, 18, 12, 0).toInstant(bogota)

    private fun reminder(
        dueDate: LocalDate,
        dueTime: LocalTime? = null,
        isPaid: Boolean = false,
    ) = Reminder(
        id = "rem-1",
        title = "Pago tarjeta Nu",
        amount = Money.ofPesos(412_000),
        dueDate = dueDate,
        dueTime = dueTime,
        recurrence = Recurrence.MONTHLY,
        isPaid = isPaid,
    )

    @Test
    fun `reminder with a time fires at that time`() {
        val trigger = reminder(LocalDate(2026, 7, 20), LocalTime(20, 0)).nextTriggerInstant(now, bogota)

        assertEquals(LocalDateTime(2026, 7, 20, 20, 0).toInstant(bogota), trigger)
    }

    @Test
    fun `date-only reminder falls back to eight in the morning`() {
        val trigger = reminder(LocalDate(2026, 7, 21)).nextTriggerInstant(now, bogota)

        assertEquals(LocalDateTime(2026, 7, 21, 8, 0).toInstant(bogota), trigger)
    }

    @Test
    fun `past-due reminder schedules nothing`() {
        assertNull(reminder(LocalDate(2026, 7, 17)).nextTriggerInstant(now, bogota))
    }

    @Test
    fun `today's reminder whose hour already passed schedules nothing`() {
        // now is 12:00; the 08:00 default already went by.
        assertNull(reminder(LocalDate(2026, 7, 18)).nextTriggerInstant(now, bogota))
    }

    @Test
    fun `today's reminder with a later time still schedules`() {
        val trigger = reminder(LocalDate(2026, 7, 18), LocalTime(20, 0)).nextTriggerInstant(now, bogota)

        assertEquals(LocalDateTime(2026, 7, 18, 20, 0).toInstant(bogota), trigger)
    }

    @Test
    fun `paid reminder schedules nothing`() {
        assertNull(reminder(LocalDate(2026, 7, 25), isPaid = true).nextTriggerInstant(now, bogota))
    }

    @Test
    fun `trigger instant is anchored to the given timezone`() {
        val utc = TimeZone.UTC
        val inBogota = reminder(LocalDate(2026, 7, 21)).nextTriggerInstant(now, bogota)
        val inUtc = reminder(LocalDate(2026, 7, 21)).nextTriggerInstant(now, utc)

        // Bogotá is UTC-5: the same wall-clock 08:00 is five hours later as an instant.
        assertEquals(5L * 60 * 60 * 1000, inBogota!!.toEpochMilliseconds() - inUtc!!.toEpochMilliseconds())
    }

    @Test
    fun `rollover interaction - marking a monthly reminder paid schedules the next month`() {
        val paid = reminder(LocalDate(2026, 7, 15), LocalTime(20, 0)).markAsPaid()

        assertEquals(
            LocalDateTime(2026, 8, 15, 20, 0).toInstant(bogota),
            paid.nextTriggerInstant(now, bogota),
        )
    }
}
