package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the duplicate warning against the two real pairs from the Sprint-3
 * brief: the $152.372 1CERO1 `Pago` ↔ Bancolombia `Pagaste` pair and the
 * $357.509 Nu ↔ Bancolombia pair. Same amount within the window flags —
 * across issuers and masks, against the ledger or another pending item.
 */
class DetectDuplicatesTest {

    private val bogota = TimeZone.of("America/Bogota")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, minute, second).toInstant(bogota)

    private fun pending(
        id: String,
        pesos: Long,
        occurredAt: Instant,
        issuer: String = "Bancolombia",
        last4: String? = "5005",
    ) = PendingTransaction(
        id = id,
        channel = CaptureChannel.SMS,
        issuer = issuer,
        rawBody = "raw",
        type = TransactionType.EXPENSE,
        amount = Money.ofPesos(pesos),
        last4 = last4,
        cardId = null,
        bank = issuer,
        merchant = null,
        occurredAt = occurredAt,
        capturedAt = occurredAt,
    )

    private fun committed(id: String, pesos: Long, occurredAt: Instant) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money.ofPesos(pesos),
        method = PaymentMethod.CARD,
        cardId = null,
        bank = "Bancolombia",
        categoryId = "cat-otros",
        merchant = null,
        occurredAt = occurredAt,
        source = TransactionSource.MANUAL,
        createdAt = occurredAt,
        updatedAt = occurredAt,
    )

    @Test
    fun `the 152372 onecero1 pago and bancolombia pagaste pair flags both pending items`() {
        val bancolombia = pending("p1", 152_372, at(2026, 7, 16, 10, 7, 26), issuer = "Bancolombia", last4 = "5005")
        val onecero1 = pending("p2", 152_372, at(2026, 7, 16, 10, 8, 8), issuer = "1CERO1", last4 = "5427")

        val flagged = detectDuplicates(listOf(bancolombia, onecero1), committed = emptyList())

        assertEquals(setOf("p1", "p2"), flagged)
    }

    @Test
    fun `the 357509 pair flags a pending item against the committed ledger`() {
        val pendingNu = pending("p1", 357_509, at(2026, 7, 16, 10, 36, 0), issuer = "Bancolombia")
        val ledger = listOf(committed("t1", 357_509, at(2026, 7, 16, 10, 34, 42)))

        val flagged = detectDuplicates(listOf(pendingNu), ledger)

        assertEquals(setOf("p1"), flagged)
    }

    @Test
    fun `same amount outside the window does not flag`() {
        val morning = pending("p1", 152_372, at(2026, 7, 16, 8, 0))
        val ledger = listOf(committed("t1", 152_372, at(2026, 7, 16, 10, 34)))

        assertTrue(detectDuplicates(listOf(morning), ledger).isEmpty())
    }

    @Test
    fun `different amounts inside the window do not flag`() {
        val a = pending("p1", 152_372, at(2026, 7, 16, 10, 7))
        val b = pending("p2", 152_373, at(2026, 7, 16, 10, 8))

        assertTrue(detectDuplicates(listOf(a, b), committed = emptyList()).isEmpty())
    }

    @Test
    fun `an item never duplicates itself`() {
        val single = pending("p1", 18_000, at(2026, 7, 9, 8, 50))

        assertTrue(detectDuplicates(listOf(single), committed = emptyList()).isEmpty())
    }
}
