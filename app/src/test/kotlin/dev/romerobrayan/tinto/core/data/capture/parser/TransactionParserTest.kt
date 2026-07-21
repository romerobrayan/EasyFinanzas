package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Table-driven tests over the verbatim sample messages in
 * TASK_SPRINT_3_CAPTURE.md and the Sprint 4 appendix — amounts in both
 * separator conventions (with and without decimals), all three date layouts,
 * relative dates falling back to receivedAt, direction, last4 extraction,
 * and the drop cases.
 */
class TransactionParserTest {

    private val parser = RuleBasedTransactionParser()
    private val bogota = TimeZone.of("America/Bogota")
    private val receivedAt = LocalDateTime(2026, 7, 16, 18, 0).toInstant(bogota)

    private fun sms(sender: String, body: String) =
        RawCapture(sender = sender, body = body, receivedAt = receivedAt, channel = CaptureChannel.SMS)

    private fun nuNotification(body: String) = RawCapture(
        sender = IssuerRules.NU_PACKAGE_NAME,
        body = body,
        receivedAt = receivedAt,
        channel = CaptureChannel.NOTIFICATION,
    )

    private fun parseRecognized(sender: String, body: String): PendingTransaction {
        val result = parser.parse(sms(sender, body))
        assertTrue("expected Recognized, got $result", result is ParseResult.Recognized)
        return (result as ParseResult.Recognized).pending
    }

    private fun parseNuRecognized(body: String): PendingTransaction {
        val result = parser.parse(nuNotification(body))
        assertTrue("expected Recognized, got $result", result is ParseResult.Recognized)
        return (result as ParseResult.Recognized).pending
    }

    private fun instantOf(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0) =
        LocalDateTime(year, month, day, hour, minute, second).toInstant(bogota)

    // ---- Bancolombia (85540) — US-style amounts, DD/MM/YY(YY) dates ----

    @Test
    fun `bancolombia bre-b llave payment stages as expense`() {
        val pending = parseRecognized(
            "85540",
            "\$15,000.00 a la llave @samueld7650 desde tu cuenta *5005 a samuel diaz " +
                "el 07/07/26 a las 14:45. Con Bre-b es de una y gratis. Dudas al 018000912345.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(15_000), pending.amount)
        assertEquals("5005", pending.last4)
        assertEquals("samuel diaz", pending.merchant)
        assertEquals(instantOf(2026, 7, 7, 14, 45), pending.occurredAt)
        assertEquals("Bancolombia", pending.issuer)
    }

    @Test
    fun `bancolombia qr payment stages as expense with the llave as merchant`() {
        val pending = parseRecognized(
            "85540",
            "Bancolombia: BRAYAN ROMERO DORADO pagaste \$18,000.00 por codigo QR desde tu cuenta " +
                "*5005 a la llave 0089378571 el 09/07/2026 a las 08:50. Con codigo QR es facil y de una.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(18_000), pending.amount)
        assertEquals("5005", pending.last4)
        assertEquals("0089378571", pending.merchant)
        assertEquals(instantOf(2026, 7, 9, 8, 50), pending.occurredAt)
    }

    @Test
    fun `bancolombia incoming transfer stages as income without decimals`() {
        val pending = parseRecognized(
            "85540",
            "Bancolombia: Recibiste una transferencia por \$10,000 de ANDRES OSORNO en tu cuenta " +
                "**5005, el 14/07/2026 a las 12:15. Si tienes dudas, hablemos: 018000931987.",
        )
        assertEquals(TransactionType.INCOME, pending.type)
        assertEquals(Money.ofPesos(10_000), pending.amount)
        assertEquals("5005", pending.last4)
        assertEquals("ANDRES OSORNO", pending.merchant)
        assertEquals(instantOf(2026, 7, 14, 12, 15), pending.occurredAt)
    }

    @Test
    fun `bancolombia consignacion stages as income with grouped no-decimal amount`() {
        val pending = parseRecognized(
            "85540",
            "Bancolombia: Recibiste una consignacion por \$1,487,941 desde el corresponsal " +
                "MULTIPAGAS UNICENTRO CENTRO CO en MEDELLIN, el 15/07/26 16:56. Si tienes dudas, llamanos.",
        )
        assertEquals(TransactionType.INCOME, pending.type)
        assertEquals(Money.ofPesos(1_487_941), pending.amount)
        assertNull(pending.last4)
        assertEquals("MULTIPAGAS UNICENTRO CENTRO CO en MEDELLIN", pending.merchant)
        assertEquals(instantOf(2026, 7, 15, 16, 56), pending.occurredAt)
    }

    @Test
    fun `bancolombia pagaste with producto mask and trailing seconds`() {
        val pending = parseRecognized(
            "85540",
            "Bancolombia: Pagaste \$152,372.00 a 101 FINTECH S A S desde tu producto 5005 " +
                "el 16/07/2026 10:07:26. ¿Dudas? Llamanos al 6045109095. Estamos cerca",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(152_372), pending.amount)
        assertEquals("5005", pending.last4)
        assertEquals("101 FINTECH S A S", pending.merchant)
        assertEquals(instantOf(2026, 7, 16, 10, 7, 26), pending.occurredAt)
    }

    @Test
    fun `bancolombia transfer out stages as expense against the target account`() {
        val pending = parseRecognized(
            "85540",
            "Bancolombia: Transferiste \$150,000.00 desde tu cuenta 5005 a la cuenta *3105687364 " +
                "el 16/07/2026 a las 16:21. ¿Dudas? Llamanos al 018000931987. Estamos cerca.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(150_000), pending.amount)
        assertEquals("5005", pending.last4)
        assertEquals("*3105687364", pending.merchant)
        assertEquals(instantOf(2026, 7, 16, 16, 21), pending.occurredAt)
    }

    // ---- 1CERO1 (891134) — Colombian amounts, YYYY/MM/DD dates ----

    @Test
    fun `onecero1 purchase with redacted merchant stages as expense`() {
        val pending = parseRecognized(
            "891134",
            "1CERO1 informa realizo Compra ..., el 2026/05/20 a las 09:11:57 por \$3.900,00 " +
                "con la TC 49846720*****5427 .",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(3_900), pending.amount)
        assertEquals("5427", pending.last4)
        assertNull(pending.merchant)
        assertEquals(instantOf(2026, 5, 20, 9, 11, 57), pending.occurredAt)
        assertEquals("1CERO1", pending.issuer)
    }

    @Test
    fun `onecero1 purchase keeps the merchant text`() {
        val pending = parseRecognized(
            "891134",
            "1CERO1 informa realizo Compra MERCADO PAGO*MERCADOL, el 2026/06/25 a las 11:16:59 " +
                "por \$851.791,00 con la TC 49846720*****5427 .",
        )
        assertEquals(Money.ofPesos(851_791), pending.amount)
        assertEquals("MERCADO PAGO*MERCADOL", pending.merchant)
        assertEquals("5427", pending.last4)
    }

    @Test
    fun `onecero1 bill payment stages as expense so the duplicate warning can flag it`() {
        val pending = parseRecognized(
            "891134",
            "1CERO1 informa realizo Pago, el 2026/07/16 a las 10:08:08 por \$152.372,00 " +
                "con la TC 49846720*****5427 .",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(152_372), pending.amount)
        assertNull(pending.merchant)
        assertEquals(instantOf(2026, 7, 16, 10, 8, 8), pending.occurredAt)
    }

    // ---- Nu (app package, NOTIFICATION) — Colombian amounts, relative dates ----

    @Test
    fun `nu card purchase stages as expense with last4 and receivedAt fallback`() {
        val pending = parseNuRecognized(
            "Compra aprobada por \$13.300,00 - Tu compra en GOOGLE YouTube por \$13.300,00 " +
                "con tu tarjeta terminada en 3101 ha sido APROBADA.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(13_300), pending.amount)
        assertEquals("3101", pending.last4)
        assertEquals("GOOGLE YouTube", pending.merchant)
        // Relative dates ("Hoy • 11:40") carry no absolute date → postTime.
        assertEquals(receivedAt, pending.occurredAt)
        assertEquals("Nu", pending.issuer)
        assertEquals("NU Bank", pending.bank)
        assertEquals(CaptureChannel.NOTIFICATION, pending.channel)
    }

    @Test
    fun `nu account payment stages as expense without a card mask`() {
        val pending = parseNuRecognized(
            "Pago aprobado por \$180.720,00 - Pagaste en GLOBAL COLOMBIA 81 SA con tu cuenta " +
                "de ahorros. Si tienes dudas contáctanos via chat.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(180_720), pending.amount)
        assertNull(pending.last4)
        assertEquals("GLOBAL COLOMBIA 81 SA", pending.merchant)
        assertEquals(receivedAt, pending.occurredAt)
    }

    @Test
    fun `nu credit card bill payment stages as expense so the duplicate warning can flag it`() {
        val pending = parseNuRecognized(
            "¡Bravo! Pagaste tu tarjeta de crédito Nu. Recibimos tu pago por \$357.509,00. " +
                "En un rato podrás verlo en tu app.",
        )
        assertEquals(TransactionType.EXPENSE, pending.type)
        assertEquals(Money.ofPesos(357_509), pending.amount)
        assertNull(pending.last4)
        assertNull(pending.merchant)
        assertEquals(receivedAt, pending.occurredAt)
    }

    @Test
    fun `nu parse still matches with the notification title prefixed to the text`() {
        val pending = parseNuRecognized(
            "Nu Compra aprobada por \$13.300,00 - Tu compra en GOOGLE YouTube por \$13.300,00 " +
                "con tu tarjeta terminada en 3101 ha sido APROBADA.",
        )
        assertEquals(Money.ofPesos(13_300), pending.amount)
        assertEquals("3101", pending.last4)
    }

    // ---- Drops ----

    @Test
    fun `nu payment request is ignored with a reason`() {
        val result = parser.parse(
            nuNotification(
                "Tienes un pago por \$180.000,00 de GLOBAL COLOMBIA 81 SA. Completa tu pago " +
                    "de forma fácil y segura en tu app Nu.",
            ),
        )
        assertTrue("expected Ignored, got $result", result is ParseResult.Ignored)
    }

    @Test
    fun `unknown package is unrecognized`() {
        val result = parser.parse(
            RawCapture(
                sender = "com.example.otherbank",
                body = "Compra aprobada por \$13.300,00 con tu tarjeta terminada en 3101",
                receivedAt = receivedAt,
                channel = CaptureChannel.NOTIFICATION,
            ),
        )
        assertEquals(ParseResult.Unrecognized, result)
    }

    @Test
    fun `onecero1 pin change is ignored with a reason`() {
        val result = parser.parse(
            sms(
                "891134",
                "1CERO1 informa realizo Cambio Clave, el 2026/05/25 a las 10:32:51 con la TC 49846720*****5427 .",
            ),
        )
        assertTrue("expected Ignored, got $result", result is ParseResult.Ignored)
    }

    @Test
    fun `unknown sender is unrecognized`() {
        val result = parser.parse(sms("890123", "Pagaste \$10,000 a ALGUIEN desde tu cuenta 1234 el 01/07/26 a las 10:00."))
        assertEquals(ParseResult.Unrecognized, result)
    }

    @Test
    fun `bancolombia marketing noise is unrecognized`() {
        val result = parser.parse(
            sms("85540", "Bancolombia: Tu clave dinamica es 123456. No la compartas con nadie."),
        )
        assertEquals(ParseResult.Unrecognized, result)
    }

    // ---- Provenance ----

    @Test
    fun `recognized parse keeps the raw body and capture timestamp`() {
        val body = "1CERO1 informa realizo Pago, el 2026/07/16 a las 10:08:08 por \$152.372,00 con la TC 49846720*****5427 ."
        val pending = parseRecognized("891134", body)
        assertEquals(body, pending.rawBody)
        assertEquals(receivedAt, pending.capturedAt)
        assertNull(pending.cardId)
        assertEquals("101Fintech", pending.bank)
    }
}
