package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Table-driven tests over the VERBATIM sample messages in the
 * TASK_SPRINT_3_CAPTURE.md appendix: amount (both separator conventions and
 * the no-decimal forms), all three date layouts against America/Bogotá,
 * direction, last4 extraction and card match, merchant, and the drop cases.
 */
class TransactionParserTest {

    private val parser = RuleBasedTransactionParser(TintoIssuerRules.all)

    private val registeredCards = listOf(
        Card(id = "card-bancolombia", bank = "Bancolombia", last4 = "5005", label = "Cuenta"),
        Card(id = "card-1cero1", bank = "1CERO1", last4 = "5427", label = "TC trabajo"),
    )

    private val receivedAt = Instant.parse("2026-07-16T17:00:00Z")

    private fun parse(sender: String, body: String): ParseResult =
        parser.parse(
            RawCapture(sender = sender, body = body, receivedAt = receivedAt, channel = CaptureChannel.SMS),
            registeredCards,
        )

    private fun recognized(sender: String, body: String): PendingTransaction {
        val result = parse(sender, body)
        assertTrue("expected Recognized for: $body", result is ParseResult.Recognized)
        return (result as ParseResult.Recognized).pending
    }

    private fun bogota(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, minute, second).toInstant(CaptureTimeZone)

    private data class Expected(
        val sender: String,
        val body: String,
        val issuer: String,
        val type: TransactionType,
        val amountCents: Long,
        val occurredAt: Instant,
        val last4: String?,
        val cardId: String?,
        val merchant: String?,
    )

    @Test
    fun `every appendix sample parses to the expected fields`() {
        val table = listOf(
            // Bancolombia (85540) — Bre-b llave payment, DD/MM/YY "a las" date.
            Expected(
                sender = "85540",
                body = "\$15,000.00 a la llave @samueld7650 desde tu cuenta *5005 a samuel diaz " +
                    "el 07/07/26 a las 14:45. Con Bre-b es de una y gratis. Dudas al 018000912345.",
                issuer = "Bancolombia",
                type = TransactionType.EXPENSE,
                amountCents = 1_500_000L,
                occurredAt = bogota(2026, 7, 7, 14, 45),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "samuel diaz",
            ),
            // QR payment to a llave, DD/MM/YYYY "a las" date.
            Expected(
                sender = "85540",
                body = "Bancolombia: BRAYAN ROMERO DORADO pagaste \$18,000.00 por codigo QR desde tu " +
                    "cuenta *5005 a la llave 0089378571 el 09/07/2026 a las 08:50. Con codigo QR es " +
                    "facil y de una. Dudas al 018000912345.",
                issuer = "Bancolombia",
                type = TransactionType.EXPENSE,
                amountCents = 1_800_000L,
                occurredAt = bogota(2026, 7, 9, 8, 50),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "0089378571",
            ),
            // Incoming transfer, no-decimal amount, **-masked account.
            Expected(
                sender = "85540",
                body = "Bancolombia: Recibiste una transferencia por \$10,000 de ANDRES OSORNO en tu " +
                    "cuenta **5005, el 14/07/2026 a las 12:15. Si tienes dudas, hablemos: " +
                    "018000931987. Siempre a tu lado.",
                issuer = "Bancolombia",
                type = TransactionType.INCOME,
                amountCents = 1_000_000L,
                occurredAt = bogota(2026, 7, 14, 12, 15),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "ANDRES OSORNO",
            ),
            // Consignación: big no-decimal amount, DD/MM/YY date WITHOUT "a las",
            // no account mask at all, city kept out of the merchant.
            Expected(
                sender = "85540",
                body = "Bancolombia: Recibiste una consignacion por \$1,487,941 desde el corresponsal " +
                    "MULTIPAGAS UNICENTRO CENTRO CO en MEDELLIN, el 15/07/26 16:56. Si tienes dudas, " +
                    "llamanos: 018000931987. A tu lado siempre.",
                issuer = "Bancolombia",
                type = TransactionType.INCOME,
                amountCents = 148_794_100L,
                occurredAt = bogota(2026, 7, 15, 16, 56),
                last4 = null,
                cardId = null,
                merchant = "MULTIPAGAS UNICENTRO CENTRO CO",
            ),
            // Pagaste with trailing HH:mm:ss timestamp and "producto" mask.
            Expected(
                sender = "85540",
                body = "Bancolombia: Pagaste \$152,372.00 a 101 FINTECH S A S desde tu producto 5005 " +
                    "el 16/07/2026 10:07:26. ¿Dudas? Llamanos al 6045109095. Estamos cerca",
                issuer = "Bancolombia",
                type = TransactionType.EXPENSE,
                amountCents = 15_237_200L,
                occurredAt = bogota(2026, 7, 16, 10, 7, 26),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "101 FINTECH S A S",
            ),
            Expected(
                sender = "85540",
                body = "Bancolombia: Pagaste \$357,509.00 a NU Compania de Financiamiento desde tu " +
                    "producto 5005 el 16/07/2026 10:34:42. ¿Dudas? Llamanos al 6045109095. Estamos cerca",
                issuer = "Bancolombia",
                type = TransactionType.EXPENSE,
                amountCents = 35_750_900L,
                occurredAt = bogota(2026, 7, 16, 10, 34, 42),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "NU Compania de Financiamiento",
            ),
            // Outgoing transfer: the DESTINATION account must not be read as the
            // user's mask (source has no asterisk here).
            Expected(
                sender = "85540",
                body = "Bancolombia: Transferiste \$150,000.00 desde tu cuenta 5005 a la cuenta " +
                    "*3105687364 el 16/07/2026 a las 16:21. ¿Dudas? Llamanos al 018000931987. " +
                    "Estamos cerca.",
                issuer = "Bancolombia",
                type = TransactionType.EXPENSE,
                amountCents = 15_000_000L,
                occurredAt = bogota(2026, 7, 16, 16, 21),
                last4 = "5005",
                cardId = "card-bancolombia",
                merchant = "cuenta *3105687364",
            ),
            // 1CERO1 (891134) — Compra with redacted merchant → null merchant,
            // Colombian separators, YYYY/MM/DD date, TC mask.
            Expected(
                sender = "891134",
                body = "1CERO1 informa realizo Compra ..., el 2026/05/20 a las 09:11:57 por " +
                    "\$3.900,00 con la TC 49846720*****5427 .",
                issuer = "1CERO1",
                type = TransactionType.EXPENSE,
                amountCents = 390_000L,
                occurredAt = bogota(2026, 5, 20, 9, 11, 57),
                last4 = "5427",
                cardId = "card-1cero1",
                merchant = null,
            ),
            Expected(
                sender = "891134",
                body = "1CERO1 informa realizo Compra MERCADO PAGO*MERCADOL, el 2026/06/25 a las " +
                    "11:16:59 por \$851.791,00 con la TC 49846720*****5427 .",
                issuer = "1CERO1",
                type = TransactionType.EXPENSE,
                amountCents = 85_179_100L,
                occurredAt = bogota(2026, 6, 25, 11, 16, 59),
                last4 = "5427",
                cardId = "card-1cero1",
                merchant = "MERCADO PAGO*MERCADOL",
            ),
            // Card-bill payment — stages as an expense; the inbox flags it as
            // the duplicate of the Bancolombia "Pagaste $152,372.00" above.
            Expected(
                sender = "891134",
                body = "1CERO1 informa realizo Pago, el 2026/07/16 a las 10:08:08 por \$152.372,00 " +
                    "con la TC 49846720*****5427 .",
                issuer = "1CERO1",
                type = TransactionType.EXPENSE,
                amountCents = 15_237_200L,
                occurredAt = bogota(2026, 7, 16, 10, 8, 8),
                last4 = "5427",
                cardId = "card-1cero1",
                merchant = null,
            ),
        )

        table.forEach { expected ->
            val pending = recognized(expected.sender, expected.body)
            assertEquals(expected.body, expected.issuer, pending.issuer)
            assertEquals(expected.body, expected.type, pending.type)
            assertEquals(expected.body, Money(expected.amountCents), pending.amount)
            assertEquals(expected.body, expected.occurredAt, pending.occurredAt)
            assertEquals(expected.body, expected.last4, pending.last4)
            assertEquals(expected.body, expected.cardId, pending.cardId)
            assertEquals(expected.body, expected.merchant, pending.merchant)
            assertEquals(expected.body, expected.body, pending.rawBody)
            assertEquals(expected.body, CaptureChannel.SMS, pending.channel)
        }
    }

    @Test
    fun `cambio clave is known noise - dropped, never staged`() {
        val result = parse(
            "891134",
            "1CERO1 informa realizo Cambio Clave, el 2026/05/25 a las 10:32:51 con la TC " +
                "49846720*****5427 .",
        )
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun `a payment request is never staged - unknown sender falls out`() {
        // Nu's "Tienes un pago" is a request, not a movement; Nu is not an SMS
        // sender this sprint, so nothing from it may ever reach the store.
        val result = parse(
            "com.nu.production",
            "Tienes un pago por \$180.000,00 de GLOBAL COLOMBIA 81 SA. Completa tu pago de " +
                "forma fácil y segura en tu app Nu.",
        )
        assertEquals(ParseResult.Unrecognized, result)
    }

    @Test
    fun `allow-listed sender with a non-transaction body is dropped silently`() {
        val marketing = parse(
            "85540",
            "Bancolombia: aprovecha tu credito preaprobado hasta \$5,000,000. Solicitalo en la app.",
        )
        assertEquals(ParseResult.Unrecognized, marketing)

        val otp = parse("85540", "Bancolombia: tu clave dinamica es 123456. No la compartas.")
        assertEquals(ParseResult.Unrecognized, otp)
    }

    @Test
    fun `body without a date falls back to receivedAt`() {
        val pending = recognized(
            "85540",
            "Bancolombia: Pagaste \$5,000.00 a ACME desde tu producto 5005.",
        )
        assertEquals(receivedAt, pending.occurredAt)
    }

    @Test
    fun `unmatched mask keeps the raw last4 and the issuer as bank`() {
        val result = parser.parse(
            RawCapture(
                sender = "891134",
                body = "1CERO1 informa realizo Pago, el 2026/07/16 a las 10:08:08 por \$152.372,00 " +
                    "con la TC 49846720*****5427 .",
                receivedAt = receivedAt,
                channel = CaptureChannel.SMS,
            ),
            registeredCards = emptyList(),
        )
        val pending = (result as ParseResult.Recognized).pending
        assertEquals("5427", pending.last4)
        assertNull(pending.cardId)
        assertEquals("1CERO1", pending.bank)
    }

    @Test
    fun `matched card sets both cardId and the card's bank`() {
        val pending = recognized(
            "85540",
            "Bancolombia: Pagaste \$152,372.00 a 101 FINTECH S A S desde tu producto 5005 " +
                "el 16/07/2026 10:07:26. ¿Dudas? Llamanos al 6045109095. Estamos cerca",
        )
        assertEquals("card-bancolombia", pending.cardId)
        assertEquals("Bancolombia", pending.bank)
    }

    @Test
    fun `sender matching tolerates a country prefix`() {
        val pending = recognized(
            "+5785540",
            "Bancolombia: Pagaste \$152,372.00 a 101 FINTECH S A S desde tu producto 5005 " +
                "el 16/07/2026 10:07:26. ¿Dudas? Llamanos al 6045109095. Estamos cerca",
        )
        assertEquals("Bancolombia", pending.issuer)
        assertTrue(TintoIssuerRules.isAllowlisted("+5785540"))
        assertTrue(TintoIssuerRules.isAllowlisted("891134"))
    }
}
