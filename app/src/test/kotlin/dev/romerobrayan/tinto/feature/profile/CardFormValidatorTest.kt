package dev.romerobrayan.tinto.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFormValidatorTest {

    @Test
    fun `valid form has no errors`() {
        val errors = CardFormValidator.validate(bank = "Bancolombia", last4 = "3092")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `blank bank is rejected`() {
        val errors = CardFormValidator.validate(bank = "   ", last4 = "3092")

        assertEquals(setOf(CardFormValidator.Error.BANK_REQUIRED), errors)
    }

    @Test
    fun `last4 must be exactly four digits`() {
        assertTrue(
            CardFormValidator.Error.LAST4_INVALID in
                CardFormValidator.validate(bank = "Nu", last4 = "309"),
        )
        assertTrue(
            CardFormValidator.Error.LAST4_INVALID in
                CardFormValidator.validate(bank = "Nu", last4 = ""),
        )
        assertTrue(
            CardFormValidator.Error.LAST4_INVALID in
                CardFormValidator.validate(bank = "Nu", last4 = "30a2"),
        )
        assertTrue(
            CardFormValidator.validate(bank = "Nu", last4 = "3092").isEmpty(),
        )
    }

    @Test
    fun `empty form reports both errors`() {
        val errors = CardFormValidator.validate(bank = "", last4 = "")

        assertEquals(
            setOf(
                CardFormValidator.Error.BANK_REQUIRED,
                CardFormValidator.Error.LAST4_INVALID,
            ),
            errors,
        )
    }
}
