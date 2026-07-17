package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.ChartBucket
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Period
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import javax.inject.Inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Aggregates transactions of one [TransactionType] (expenses by default) into
 * the dashboard chart buckets. The last bucket is the one containing [anchor];
 * earlier buckets walk backwards at the granularity of [period]. Bucketing is
 * date math here — not SQL — so it stays testable and locale-safe.
 */
class AggregateSpendUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<Transaction>,
        period: Period,
        anchor: LocalDate,
        timeZone: TimeZone,
        type: TransactionType = TransactionType.EXPENSE,
    ): List<ChartBucket> {
        val starts: List<LocalDate> = when (period) {
            Period.DAY -> (BUCKETS_PER_DAY_CHART - 1 downTo 0)
                .map { anchor.minus(it, DateTimeUnit.DAY) }

            Period.WEEK -> (BUCKETS_PER_WEEK_CHART - 1 downTo 0)
                .map { anchor.startOfWeek().minus(it * 7, DateTimeUnit.DAY) }

            Period.MONTH -> (BUCKETS_PER_MONTH_CHART - 1 downTo 0)
                .map { anchor.startOfMonth().minus(it, DateTimeUnit.MONTH) }

            Period.YEAR -> (BUCKETS_PER_YEAR_CHART - 1 downTo 0)
                .map { LocalDate(anchor.year - it, 1, 1) }
        }

        val amountsByDate = transactions
            .filter { it.type == type }
            .map { it.occurredAt.toLocalDateTime(timeZone).date to it.amount }

        return starts.map { start ->
            val end = start.nextBucketStart(period)
            val total = amountsByDate
                .filter { (date, _) -> date >= start && date < end }
                .fold(Money.Zero) { acc, (_, amount) -> acc + amount }
            ChartBucket(start = start, endExclusive = end, total = total)
        }
    }

    private fun LocalDate.nextBucketStart(period: Period): LocalDate = when (period) {
        Period.DAY -> plus(1, DateTimeUnit.DAY)
        Period.WEEK -> plus(7, DateTimeUnit.DAY)
        Period.MONTH -> plus(1, DateTimeUnit.MONTH)
        Period.YEAR -> plus(1, DateTimeUnit.YEAR)
    }

    companion object {
        const val BUCKETS_PER_DAY_CHART = 7
        const val BUCKETS_PER_WEEK_CHART = 6
        const val BUCKETS_PER_MONTH_CHART = 7
        const val BUCKETS_PER_YEAR_CHART = 4
    }
}

/** Monday-based start of week (ISO). */
fun LocalDate.startOfWeek(): LocalDate = minus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

fun LocalDate.startOfMonth(): LocalDate = LocalDate(year, monthNumber, 1)
