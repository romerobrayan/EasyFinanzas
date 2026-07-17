package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Table test over every sample amount in the TASK_SPRINT_3_CAPTURE.md
 * appendix, in both separator conventions plus the no-decimal forms, and the
 * unknown-sender heuristic. All integer math — a wrong centavo here is a
 * corrupted ledger later.
 */
class AmountParserTest {

    private data class Row(
        val text: String,
        val convention: DecimalConvention?,
        val expectedCents: Long,
    )

    @Test
    fun `bancolombia us-style amounts parse to exact cents`() {
        val rows = listOf(
            Row("\$15,000.00", DecimalConvention.DOT_DECIMAL, 1_500_000L),
            Row("\$18,000.00", DecimalConvention.DOT_DECIMAL, 1_800_000L),
            Row("\$10,000", DecimalConvention.DOT_DECIMAL, 1_000_000L),
            Row("\$1,487,941", DecimalConvention.DOT_DECIMAL, 148_794_100L),
            Row("\$152,372.00", DecimalConvention.DOT_DECIMAL, 15_237_200L),
            Row("\$357,509.00", DecimalConvention.DOT_DECIMAL, 35_750_900L),
            Row("\$150,000.00", DecimalConvention.DOT_DECIMAL, 15_000_000L),
        )
        rows.forEach { row ->
            assertEquals(row.text, Money(row.expectedCents), AmountParser.parse(row.text, row.convention))
        }
    }

    @Test
    fun `colombian-style amounts parse to exact cents`() {
        val rows = listOf(
            Row("\$3.900,00", DecimalConvention.COMMA_DECIMAL, 390_000L),
            Row("\$851.791,00", DecimalConvention.COMMA_DECIMAL, 85_179_100L),
            Row("\$152.372,00", DecimalConvention.COMMA_DECIMAL, 15_237_200L),
            Row("\$180.720,00", DecimalConvention.COMMA_DECIMAL, 18_072_000L),
            Row("\$13.300,00", DecimalConvention.COMMA_DECIMAL, 1_330_000L),
            Row("\$357.509,00", DecimalConvention.COMMA_DECIMAL, 35_750_900L),
            Row("\$180.000,00", DecimalConvention.COMMA_DECIMAL, 18_000_000L),
        )
        rows.forEach { row ->
            assertEquals(row.text, Money(row.expectedCents), AmountParser.parse(row.text, row.convention))
        }
    }

    @Test
    fun `unknown sender heuristic - two trailing digits after the last separator are the decimals`() {
        val rows = listOf(
            Row("\$15,000.00", null, 1_500_000L),
            Row("\$3.900,00", null, 390_000L),
            Row("\$1,50", null, 150L),
        )
        rows.forEach { row ->
            assertEquals(row.text, Money(row.expectedCents), AmountParser.parse(row.text, row.convention))
        }
    }

    @Test
    fun `unknown sender heuristic - everything else groups`() {
        val rows = listOf(
            Row("\$10,000", null, 1_000_000L),
            Row("\$1.487.941", null, 148_794_100L),
            Row("\$152.372", null, 15_237_200L),
        )
        rows.forEach { row ->
            assertEquals(row.text, Money(row.expectedCents), AmountParser.parse(row.text, row.convention))
        }
    }

    @Test
    fun `trailing sentence punctuation is not part of the amount`() {
        assertEquals(Money(1_000_000L), AmountParser.parse("\$10,000.", DecimalConvention.DOT_DECIMAL))
    }

    @Test
    fun `malformed tokens drop to null instead of misparsing`() {
        assertNull(AmountParser.parse("$", DecimalConvention.DOT_DECIMAL))
        assertNull(AmountParser.parse("sin monto", DecimalConvention.DOT_DECIMAL))
        // One decimal digit is not a COP amount (always two when present).
        assertNull(AmountParser.parse("\$3.900,0", DecimalConvention.COMMA_DECIMAL))
        // Wrong convention for the text must fail, never silently misparse.
        assertNull(AmountParser.parse("\$3.900,00", DecimalConvention.DOT_DECIMAL))
    }

    @Test
    fun `findAmount takes the first dollar token in the body`() {
        val body = "Pagaste \$152,372.00 a 101 FINTECH S A S desde tu producto 5005"
        assertEquals(Money(15_237_200L), AmountParser.findAmount(body, DecimalConvention.DOT_DECIMAL))
        assertNull(AmountParser.findAmount("realizo Cambio Clave, sin monto", DecimalConvention.COMMA_DECIMAL))
    }
}
