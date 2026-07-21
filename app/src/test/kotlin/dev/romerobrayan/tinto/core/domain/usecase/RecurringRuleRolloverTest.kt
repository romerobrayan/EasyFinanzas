package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the recurring-rule rollover. MONTHLY clamps the day-of-month for free;
 * SEMIMONTHLY ("quincenal") alternates between the 15th and the last day of
 * each month on fixed calendar dates.
 */
class RecurringRuleRolloverTest {

    private fun rule(next: LocalDate, frequency: TransactionFrequency) = RecurringRule(
        id = "r1",
        type = TransactionType.EXPENSE,
        amount = Money.ofPesos(100_000),
        method = PaymentMethod.CASH,
        cardId = null,
        bank = null,
        categoryId = "cat-otros",
        merchant = null,
        frequency = frequency,
        anchorDate = next,
        nextOccurrence = next,
        isActive = true,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun advance(next: LocalDate, frequency: TransactionFrequency): LocalDate =
        rule(next, frequency).advanced().nextOccurrence

    // --- DAILY / WEEKLY / MONTHLY ---

    @Test
    fun `daily advances one day across a year boundary`() {
        assertEquals(LocalDate(2027, 1, 1), advance(LocalDate(2026, 12, 31), TransactionFrequency.DAILY))
    }

    @Test
    fun `weekly advances seven days across a month boundary`() {
        assertEquals(LocalDate(2026, 8, 5), advance(LocalDate(2026, 7, 29), TransactionFrequency.WEEKLY))
    }

    @Test
    fun `monthly advances one month and clamps january 31 to february 28`() {
        assertEquals(LocalDate(2026, 2, 28), advance(LocalDate(2026, 1, 31), TransactionFrequency.MONTHLY))
    }

    @Test
    fun `monthly clamps to february 29 on leap years`() {
        assertEquals(LocalDate(2028, 2, 29), advance(LocalDate(2028, 1, 31), TransactionFrequency.MONTHLY))
    }

    // --- SEMIMONTHLY (quincenal): 15 ↔ last day ---

    @Test
    fun `semimonthly walks the 15-last day series through february`() {
        var date = LocalDate(2026, 1, 15)
        val series = buildList {
            repeat(5) {
                date = advance(date, TransactionFrequency.SEMIMONTHLY)
                add(date)
            }
        }
        assertEquals(
            listOf(
                LocalDate(2026, 1, 31),
                LocalDate(2026, 2, 15),
                LocalDate(2026, 2, 28),
                LocalDate(2026, 3, 15),
                LocalDate(2026, 3, 31),
            ),
            series,
        )
    }

    @Test
    fun `semimonthly lands on february 29 in a leap year`() {
        assertEquals(
            LocalDate(2028, 2, 29),
            advance(LocalDate(2028, 2, 15), TransactionFrequency.SEMIMONTHLY),
        )
        assertEquals(
            LocalDate(2028, 3, 15),
            advance(LocalDate(2028, 2, 29), TransactionFrequency.SEMIMONTHLY),
        )
    }

    @Test
    fun `semimonthly crosses the year boundary from december last day`() {
        assertEquals(
            LocalDate(2026, 1, 15),
            advance(LocalDate(2025, 12, 31), TransactionFrequency.SEMIMONTHLY),
        )
    }

    // --- nextHalfMonth snap (used to seed a rule's first occurrence) ---

    @Test
    fun `nextHalfMonth snaps before the 15th to the 15th`() {
        assertEquals(LocalDate(2026, 3, 15), LocalDate(2026, 3, 10).nextHalfMonth())
    }

    @Test
    fun `nextHalfMonth snaps the 15th to the last day`() {
        assertEquals(LocalDate(2026, 3, 31), LocalDate(2026, 3, 15).nextHalfMonth())
    }

    @Test
    fun `nextHalfMonth snaps mid-late month to the last day`() {
        assertEquals(LocalDate(2026, 3, 31), LocalDate(2026, 3, 20).nextHalfMonth())
    }

    @Test
    fun `nextHalfMonth snaps the last day to the 15th of next month`() {
        assertEquals(LocalDate(2026, 3, 15), LocalDate(2026, 2, 28).nextHalfMonth())
    }
}
