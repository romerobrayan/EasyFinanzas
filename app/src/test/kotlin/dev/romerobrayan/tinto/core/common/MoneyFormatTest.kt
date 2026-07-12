package dev.romerobrayan.tinto.core.common

import dev.romerobrayan.tinto.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun `formats zero`() {
        assertEquals("$0", MoneyFormat.format(Money.Zero))
    }

    @Test
    fun `groups thousands with dots`() {
        assertEquals("$1.842.500", MoneyFormat.format(Money.ofPesos(1_842_500)))
    }

    @Test
    fun `formats amounts under one thousand without grouping`() {
        assertEquals("$950", MoneyFormat.format(Money.ofPesos(950)))
    }

    @Test
    fun `formats small grouped amounts`() {
        assertEquals("$6.500", MoneyFormat.format(Money.ofPesos(6_500)))
    }

    @Test
    fun `always formats the absolute value`() {
        assertEquals("$25.000", MoneyFormat.format(Money.ofPesos(-25_000)))
    }

    @Test
    fun `drops centavos for display`() {
        assertEquals("$1.999", MoneyFormat.format(Money(199_950)))
    }

    @Test
    fun `money arithmetic stays in minor units`() {
        assertEquals(Money(300), Money(100) + Money(200))
        assertEquals(Money(-100), Money(100) - Money(200))
        assertEquals(Money(100), Money(-100).abs())
    }
}
