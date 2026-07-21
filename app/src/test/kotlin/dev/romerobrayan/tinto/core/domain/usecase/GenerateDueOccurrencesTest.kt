package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateDueOccurrencesTest {

    private val timeZone = TimeZone.UTC
    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000)

    private fun rule(
        nextOccurrence: LocalDate,
        frequency: TransactionFrequency = TransactionFrequency.MONTHLY,
        isActive: Boolean = true,
    ) = RecurringRule(
        id = "r1",
        type = TransactionType.INCOME,
        amount = Money.ofPesos(2_850_000),
        method = PaymentMethod.TRANSFER,
        cardId = null,
        bank = null,
        categoryId = "cat-nomina",
        merchant = "Salario",
        frequency = frequency,
        anchorDate = nextOccurrence,
        nextOccurrence = nextOccurrence,
        isActive = isActive,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `no occurrences when nextOccurrence is in the future`() {
        val r = rule(LocalDate(2026, 8, 15))
        val result = generateDueOccurrences(r, today = LocalDate(2026, 7, 21), timeZone, now)

        assertTrue(result.transactions.isEmpty())
        // Rule is returned untouched so it never advances past a future date.
        assertEquals(LocalDate(2026, 8, 15), result.advancedRule.nextOccurrence)
    }

    @Test
    fun `catch-up materializes every occurrence up to today`() {
        val r = rule(LocalDate(2026, 1, 15))
        val result = generateDueOccurrences(r, today = LocalDate(2026, 4, 20), timeZone, now)

        assertEquals(
            listOf(
                "auto-r1-20260115",
                "auto-r1-20260215",
                "auto-r1-20260315",
                "auto-r1-20260415",
            ),
            result.transactions.map { it.id },
        )
        // The advanced rule points strictly past today.
        assertEquals(LocalDate(2026, 5, 15), result.advancedRule.nextOccurrence)
        assertTrue(result.transactions.all { it.source == TransactionSource.RECURRING })
        assertTrue(result.transactions.all { it.type == TransactionType.INCOME })
    }

    @Test
    fun `deterministic ids make a same-day re-run idempotent`() {
        val r = rule(LocalDate(2026, 1, 15))
        val today = LocalDate(2026, 3, 20)
        val first = generateDueOccurrences(r, today, timeZone, now).transactions.map { it.id }
        val second = generateDueOccurrences(r, today, timeZone, now).transactions.map { it.id }

        assertEquals(first, second)
        assertEquals(listOf("auto-r1-20260115", "auto-r1-20260215", "auto-r1-20260315"), first)
    }

    @Test
    fun `semimonthly catch-up follows the 15-last day slots`() {
        val r = rule(LocalDate(2026, 1, 15), frequency = TransactionFrequency.SEMIMONTHLY)
        val result = generateDueOccurrences(r, today = LocalDate(2026, 2, 20), timeZone, now)

        assertEquals(
            listOf("auto-r1-20260115", "auto-r1-20260131", "auto-r1-20260215"),
            result.transactions.map { it.id },
        )
        assertEquals(LocalDate(2026, 2, 28), result.advancedRule.nextOccurrence)
    }

    @Test
    fun `an inactive rule generates nothing`() {
        val r = rule(LocalDate(2026, 1, 15), isActive = false)
        val result = generateDueOccurrences(r, today = LocalDate(2026, 4, 20), timeZone, now)

        assertTrue(result.transactions.isEmpty())
        assertEquals(LocalDate(2026, 1, 15), result.advancedRule.nextOccurrence)
    }
}
