package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Table tests over every sample amount in the Sprint-3 appendix, both
 * separator conventions, plus the unknown-sender heuristic. Integer math
 * only — a failure here means money corruption, not a cosmetic bug.
 */
class AmountParserTest {

    private fun pesos(value: Long) = Money.ofPesos(value)

    @Test
    fun `us style amounts parse with dot decimals`() {
        val convention = AmountConvention.DOT_DECIMAL
        assertEquals(pesos(15_000), AmountParser.parse("$15,000.00", convention))
        assertEquals(pesos(18_000), AmountParser.parse("18,000.00", convention))
        assertEquals(pesos(10_000), AmountParser.parse("$10,000", convention))
        assertEquals(pesos(1_487_941), AmountParser.parse("$1,487,941", convention))
        assertEquals(pesos(152_372), AmountParser.parse("$152,372.00", convention))
        assertEquals(pesos(357_509), AmountParser.parse("$357,509.00", convention))
        assertEquals(pesos(150_000), AmountParser.parse("$150,000.00", convention))
    }

    @Test
    fun `colombian style amounts parse with comma decimals`() {
        val convention = AmountConvention.COMMA_DECIMAL
        assertEquals(pesos(3_900), AmountParser.parse("$3.900,00", convention))
        assertEquals(pesos(851_791), AmountParser.parse("$851.791,00", convention))
        assertEquals(pesos(152_372), AmountParser.parse("$152.372,00", convention))
        assertEquals(pesos(180_720), AmountParser.parse("$180.720,00", convention))
        assertEquals(pesos(13_300), AmountParser.parse("$13.300,00", convention))
    }

    @Test
    fun `centavos survive as minor units`() {
        assertEquals(Money(1_50), AmountParser.parse("$1.50", AmountConvention.DOT_DECIMAL))
        assertEquals(Money(1_50), AmountParser.parse("$1,50", AmountConvention.COMMA_DECIMAL))
    }

    @Test
    fun `unknown convention heuristic reads two trailing digits as decimals`() {
        assertEquals(pesos(152_372), AmountParser.parse("$152,372.00", null))
        assertEquals(pesos(152_372), AmountParser.parse("$152.372,00", null))
        assertEquals(pesos(1_487_941), AmountParser.parse("$1,487,941", null))
        assertEquals(pesos(3_900), AmountParser.parse("$3.900,00", null))
        assertEquals(pesos(10_000), AmountParser.parse("$10.000", null))
    }

    @Test
    fun `garbage never parses`() {
        assertNull(AmountParser.parse("", AmountConvention.DOT_DECIMAL))
        assertNull(AmountParser.parse("$", AmountConvention.DOT_DECIMAL))
        assertNull(AmountParser.parse("12a34", AmountConvention.DOT_DECIMAL))
        assertNull(AmountParser.parse("1.2.3,4,5", AmountConvention.COMMA_DECIMAL))
    }
}
