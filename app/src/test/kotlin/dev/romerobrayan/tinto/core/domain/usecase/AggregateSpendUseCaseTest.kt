package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Period
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class AggregateSpendUseCaseTest {

    private val useCase = AggregateSpendUseCase()
    private val zone = TimeZone.of("America/Bogota")

    /** 2026-07-11 is a Saturday; its ISO week starts Monday 2026-07-06. */
    private val anchor = LocalDate(2026, 7, 11)

    private fun transaction(
        date: LocalDate,
        pesos: Long,
        type: TransactionType = TransactionType.EXPENSE,
    ): Transaction {
        val instant = date.atTime(12, 0).toInstant(zone)
        return Transaction(
            id = "$date-$pesos-$type",
            type = type,
            amount = Money.ofPesos(pesos),
            method = PaymentMethod.CASH,
            cardId = null,
            bank = null,
            categoryId = "otros",
            merchant = null,
            occurredAt = instant,
            source = TransactionSource.MANUAL,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    @Test
    fun `month chart has seven buckets ending in the anchor month`() {
        val buckets = useCase(emptyList(), Period.MONTH, anchor, zone)

        assertEquals(7, buckets.size)
        assertEquals(LocalDate(2026, 1, 1), buckets.first().start)
        assertEquals(LocalDate(2026, 7, 1), buckets.last().start)
        assertEquals(LocalDate(2026, 8, 1), buckets.last().endExclusive)
    }

    @Test
    fun `expenses land in their month bucket and income is excluded`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 3), 50_000),
            transaction(LocalDate(2026, 7, 11), 25_000),
            transaction(LocalDate(2026, 6, 20), 80_000),
            transaction(LocalDate(2026, 7, 5), 999_000, type = TransactionType.INCOME),
        )

        val buckets = useCase(transactions, Period.MONTH, anchor, zone)

        assertEquals(Money.ofPesos(75_000), buckets.last().total)
        assertEquals(Money.ofPesos(80_000), buckets[5].total)
    }

    @Test
    fun `income aggregation counts income and excludes expenses`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 3), 50_000),
            transaction(LocalDate(2026, 7, 5), 999_000, type = TransactionType.INCOME),
            transaction(LocalDate(2026, 6, 1), 850_000, type = TransactionType.INCOME),
        )

        val buckets = useCase(transactions, Period.MONTH, anchor, zone, TransactionType.INCOME)

        assertEquals(Money.ofPesos(999_000), buckets.last().total)
        assertEquals(Money.ofPesos(850_000), buckets[5].total)
    }

    @Test
    fun `day chart covers exactly the last seven days`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 5), 10_000),
            transaction(LocalDate(2026, 7, 4), 99_000),
            transaction(LocalDate(2026, 7, 11), 20_000),
        )

        val buckets = useCase(transactions, Period.DAY, anchor, zone)

        assertEquals(7, buckets.size)
        assertEquals(LocalDate(2026, 7, 5), buckets.first().start)
        assertEquals(Money.ofPesos(10_000), buckets.first().total)
        assertEquals(Money.ofPesos(20_000), buckets.last().total)
        val chartTotal = buckets.fold(Money.Zero) { acc, bucket -> acc + bucket.total }
        assertEquals(Money.ofPesos(30_000), chartTotal)
    }

    @Test
    fun `week buckets start on monday`() {
        val buckets = useCase(emptyList(), Period.WEEK, anchor, zone)

        assertEquals(6, buckets.size)
        assertEquals(LocalDate(2026, 7, 6), buckets.last().start)
        buckets.forEach { bucket ->
            assertEquals(1, bucket.start.dayOfWeek.isoDayNumber)
        }
    }

    @Test
    fun `sunday belongs to the previous monday-based week`() {
        val transactions = listOf(
            transaction(LocalDate(2026, 7, 5), 40_000),
            transaction(LocalDate(2026, 7, 6), 15_000),
        )

        val buckets = useCase(transactions, Period.WEEK, anchor, zone)

        assertEquals(Money.ofPesos(15_000), buckets.last().total)
        assertEquals(Money.ofPesos(40_000), buckets[4].total)
    }

    @Test
    fun `year chart spans four years`() {
        val transactions = listOf(transaction(LocalDate(2025, 12, 24), 180_000))

        val buckets = useCase(transactions, Period.YEAR, anchor, zone)

        assertEquals(4, buckets.size)
        assertEquals(LocalDate(2023, 1, 1), buckets.first().start)
        assertEquals(Money.ofPesos(180_000), buckets[2].total)
    }
}
