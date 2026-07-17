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
 * Pins the duplicate warning on the two real pairs from the brief: the
 * $152.372 card-bill payment (1CERO1 ↔ Bancolombia) and the $357.509 Nu
 * payment (Nu ↔ Bancolombia). Same money, two issuers, different masks —
 * the mask deliberately does not gate the match.
 */
class DetectPendingDuplicatesTest {

    private val bogota = TimeZone.of("America/Bogota")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, minute, second).toInstant(bogota)

    private fun pending(
        id: String,
        pesos: Long,
        occurredAt: Instant,
        issuer: String = "Bancolombia",
        last4: String? = "5005",
        type: TransactionType = TransactionType.EXPENSE,
    ) = PendingTransaction(
        id = id,
        channel = CaptureChannel.SMS,
        issuer = issuer,
        rawBody = "raw",
        type = type,
        amount = Money.ofPesos(pesos),
        last4 = last4,
        cardId = null,
        bank = issuer,
        merchant = null,
        occurredAt = occurredAt,
        capturedAt = occurredAt,
    )

    private fun committed(
        id: String,
        pesos: Long,
        occurredAt: Instant,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = id,
        type = type,
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
    fun `the 152372 pair flags across issuers despite different masks`() {
        // 1CERO1 "realizo Pago ... $152.372,00 con la TC ...5427" at 10:08:08,
        // still pending; its Bancolombia cash leg ("Pagaste $152,372.00 ...
        // producto 5005" at 10:07:26) was already confirmed.
        val unoCeroUnoPago = pending(
            id = "pending-1cero1",
            pesos = 152_372,
            occurredAt = at(2026, 7, 16, 10, 8, 8),
            issuer = "1CERO1",
            last4 = "5427",
        )
        val bancolombiaPagaste = committed(
            id = "tx-bancolombia",
            pesos = 152_372,
            occurredAt = at(2026, 7, 16, 10, 7, 26),
        )

        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(unoCeroUnoPago),
            committed = listOf(bancolombiaPagaste),
        )

        assertEquals(setOf("pending-1cero1"), duplicates)
    }

    @Test
    fun `the 357509 pair flags when both sides are still pending`() {
        // Bancolombia "Pagaste $357,509.00 a NU ..." and the Nu-side payment
        // confirmation for the same bill, both sitting in the inbox.
        val bancolombiaSide = pending(
            id = "pending-bancolombia",
            pesos = 357_509,
            occurredAt = at(2026, 7, 16, 10, 34, 42),
        )
        val nuSide = pending(
            id = "pending-nu",
            pesos = 357_509,
            occurredAt = at(2026, 7, 16, 10, 36, 0),
            issuer = "Nu",
            last4 = null,
        )

        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(bancolombiaSide, nuSide),
            committed = emptyList(),
        )

        assertEquals(setOf("pending-bancolombia", "pending-nu"), duplicates)
    }

    @Test
    fun `exactly the window apart still flags`() {
        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(pending("p1", 50_000, at(2026, 7, 16, 10, 0))),
            committed = listOf(committed("t1", 50_000, at(2026, 7, 16, 10, 10))),
        )
        assertEquals(setOf("p1"), duplicates)
    }

    @Test
    fun `same amount outside the window does not flag`() {
        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(pending("p1", 50_000, at(2026, 7, 16, 10, 0))),
            committed = listOf(committed("t1", 50_000, at(2026, 7, 16, 10, 20))),
        )
        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun `different amount at the same minute does not flag`() {
        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(pending("p1", 50_000, at(2026, 7, 16, 10, 0))),
            committed = listOf(committed("t1", 50_001, at(2026, 7, 16, 10, 0))),
        )
        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun `income and expense of the same amount do not cross-flag`() {
        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(pending("p1", 50_000, at(2026, 7, 16, 10, 0), type = TransactionType.INCOME)),
            committed = listOf(committed("t1", 50_000, at(2026, 7, 16, 10, 0))),
        )
        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun `a lone pending item never flags against itself`() {
        val duplicates = DetectPendingDuplicates.duplicateIds(
            pending = listOf(pending("p1", 50_000, at(2026, 7, 16, 10, 0))),
            committed = emptyList(),
        )
        assertTrue(duplicates.isEmpty())
    }
}
