package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the figures the spoken summary reads out. Getting these wrong is worse
 * than a wrong label on screen — the user has no chart to sanity-check against
 * while listening.
 */
class SummarizeMonthUseCaseTest {

    private val useCase = SummarizeMonthUseCase()
    private val zone = TimeZone.of("America/Bogota")
    private val july = LocalDate(2026, 7, 1)

    private val categories = listOf(
        category("mercado", "Mercado"),
        category("transporte", "Transporte"),
    )

    @Test
    fun `total counts only the target month's expenses`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 3), 50_000),
            transaction(LocalDate(2026, 7, 28), 30_000),
            transaction(LocalDate(2026, 6, 30), 90_000),
            transaction(LocalDate(2026, 8, 1), 70_000),
        )

        val summary = useCase(transactions, categories, july, zone)

        assertEquals(Money.ofPesos(80_000), summary.total)
        assertEquals(july, summary.month)
    }

    @Test
    fun `income is excluded from the total`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 3), 50_000),
            transaction(LocalDate(2026, 7, 4), 900_000, type = TransactionType.INCOME),
        )

        val summary = useCase(transactions, categories, july, zone)

        assertEquals(Money.ofPesos(50_000), summary.total)
    }

    @Test
    fun `any day of the month resolves to the same summary`() {
        val transactions = listOf(transaction(LocalDate(2026, 7, 3), 50_000))

        val fromMidMonth = useCase(transactions, categories, LocalDate(2026, 7, 19), zone)

        assertEquals(july, fromMidMonth.month)
        assertEquals(Money.ofPesos(50_000), fromMidMonth.total)
    }

    @Test
    fun `month boundaries are resolved in the given time zone`() {
        // 2026-08-01T00:30Z is still 2026-07-31 19:30 in Bogota (UTC-5).
        val lateJuly = LocalDate(2026, 7, 31).atTime(19, 30).toInstant(zone)
        val transactions = listOf(transaction(LocalDate(2026, 7, 3), 10_000).copy(occurredAt = lateJuly))

        val summary = useCase(transactions, categories, july, zone)

        assertEquals(Money.ofPesos(10_000), summary.total)
    }

    @Test
    fun `comparison reports a decrease against the previous month`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 6, 10), 100_000),
            transaction(LocalDate(2026, 7, 10), 88_000),
        )

        val comparison = useCase(transactions, categories, july, zone).comparison

        assertEquals(LocalDate(2026, 6, 1), comparison?.previousMonth)
        assertEquals(12, comparison?.percent)
        assertEquals(true, comparison?.isDecrease)
    }

    @Test
    fun `comparison reports an increase against the previous month`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 6, 10), 100_000),
            transaction(LocalDate(2026, 7, 10), 150_000),
        )

        val comparison = useCase(transactions, categories, july, zone).comparison

        assertEquals(50, comparison?.percent)
        assertEquals(false, comparison?.isDecrease)
    }

    @Test
    fun `no previous spend means no comparison rather than a divide by zero`() {
        val transactions = listOf(transaction(LocalDate(2026, 7, 10), 150_000))

        val summary = useCase(transactions, categories, july, zone)

        assertNull(summary.comparison)
    }

    @Test
    fun `top category resolves its display name and picks the true maximum`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 2), 30_000, categoryId = "mercado"),
            transaction(LocalDate(2026, 7, 9), 25_000, categoryId = "mercado"),
            transaction(LocalDate(2026, 7, 9), 40_000, categoryId = "transporte"),
        )

        val topCategory = useCase(transactions, categories, july, zone).topCategory

        assertEquals("Mercado", topCategory?.categoryName)
        assertEquals(Money.ofPesos(55_000), topCategory?.total)
    }

    @Test
    fun `top category ignores a category that no longer exists`() {
        val transactions = listOf(transaction(LocalDate(2026, 7, 2), 30_000, categoryId = "deleted"))

        val summary = useCase(transactions, categories, july, zone)

        assertNull(summary.topCategory)
        // The total still counts it — the money was spent either way.
        assertEquals(Money.ofPesos(30_000), summary.total)
    }

    @Test
    fun `an empty month summarizes to zero with nothing to compare or highlight`() {
        val summary = useCase(emptyList(), categories, july, zone)

        assertTrue(summary.total.isZero)
        assertNull(summary.comparison)
        assertNull(summary.topCategory)
    }

    private fun category(id: String, name: String) = Category(
        id = id,
        name = name,
        iconKey = id,
        colorHex = "#B23A5E",
        isSystem = true,
    )

    private fun transaction(
        date: LocalDate,
        pesos: Long,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String = "mercado",
    ): Transaction {
        val instant = date.atTime(12, 0).toInstant(zone)
        return Transaction(
            id = "$date-$pesos-$type-$categoryId",
            type = type,
            amount = Money.ofPesos(pesos),
            method = PaymentMethod.CASH,
            cardId = null,
            bank = null,
            categoryId = categoryId,
            merchant = null,
            occurredAt = instant,
            source = TransactionSource.MANUAL,
            createdAt = instant,
            updatedAt = instant,
        )
    }
}
