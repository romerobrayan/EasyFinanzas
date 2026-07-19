package dev.romerobrayan.tinto.core.data.local

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The dedup-key discipline per channel: SMS keys on the exact millisecond
 * (distinct messages can share a body), while NOTIFICATION buckets the
 * timestamp so a re-posted notification with a nudged postTime collapses
 * onto the same key and never re-stages (or resurrects) an item.
 */
class PendingTransactionDedupKeyTest {

    // Chosen so +30s stays inside the same 10-minute bucket.
    private val baseInstant = Instant.fromEpochMilliseconds(1_000_000_000_000)

    private fun pending(
        channel: CaptureChannel,
        occurredAt: Instant,
        body: String = "Compra aprobada por \$13.300,00 con tu tarjeta terminada en 3101",
    ) = PendingTransaction(
        id = "id",
        channel = channel,
        issuer = if (channel == CaptureChannel.NOTIFICATION) "Nu" else "Bancolombia",
        rawBody = body,
        type = TransactionType.EXPENSE,
        amount = Money.ofPesos(13_300),
        last4 = "3101",
        cardId = null,
        bank = null,
        merchant = null,
        occurredAt = occurredAt,
        capturedAt = occurredAt,
    )

    @Test
    fun `re-posted notification with a nudged postTime keeps the same key`() {
        val first = pending(CaptureChannel.NOTIFICATION, baseInstant)
        val repost = pending(CaptureChannel.NOTIFICATION, Instant.fromEpochMilliseconds(1_000_000_030_000))
        assertEquals(
            PendingTransactionEntity.dedupKeyOf(first),
            PendingTransactionEntity.dedupKeyOf(repost),
        )
    }

    @Test
    fun `notifications with different content never share a key`() {
        val purchase = pending(CaptureChannel.NOTIFICATION, baseInstant)
        val other = pending(
            CaptureChannel.NOTIFICATION,
            baseInstant,
            body = "Pago aprobado por \$180.720,00 - Pagaste en GLOBAL COLOMBIA 81 SA con tu cuenta de ahorros.",
        )
        assertNotEquals(
            PendingTransactionEntity.dedupKeyOf(purchase),
            PendingTransactionEntity.dedupKeyOf(other),
        )
    }

    @Test
    fun `sms keeps exact-millisecond keys so near-identical messages stay distinct`() {
        val first = pending(CaptureChannel.SMS, baseInstant)
        val second = pending(CaptureChannel.SMS, Instant.fromEpochMilliseconds(1_000_000_030_000))
        assertNotEquals(
            PendingTransactionEntity.dedupKeyOf(first),
            PendingTransactionEntity.dedupKeyOf(second),
        )
    }
}
