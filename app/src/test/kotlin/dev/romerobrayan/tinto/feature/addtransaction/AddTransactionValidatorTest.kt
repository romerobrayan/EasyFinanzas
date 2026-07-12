package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionValidator.Error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionValidatorTest {

    @Test
    fun `valid card expense passes`() {
        val errors = AddTransactionValidator.validate(
            amountPesos = 25_000,
            method = PaymentMethod.CARD,
            last4 = "3092",
            categoryId = "comida",
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `cash does not require last4`() {
        val errors = AddTransactionValidator.validate(
            amountPesos = 10_000,
            method = PaymentMethod.CASH,
            last4 = "",
            categoryId = "comida",
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `zero or missing amount is rejected`() {
        assertEquals(
            setOf(Error.AMOUNT_REQUIRED),
            AddTransactionValidator.validate(0, PaymentMethod.CASH, "", "comida"),
        )
        assertEquals(
            setOf(Error.AMOUNT_REQUIRED),
            AddTransactionValidator.validate(null, PaymentMethod.CASH, "", "comida"),
        )
    }

    @Test
    fun `card requires exactly four digits`() {
        val tooShort = AddTransactionValidator.validate(5_000, PaymentMethod.CARD, "309", "comida")
        val nonDigits = AddTransactionValidator.validate(5_000, PaymentMethod.CARD, "30a2", "comida")
        assertEquals(setOf(Error.LAST4_INVALID), tooShort)
        assertEquals(setOf(Error.LAST4_INVALID), nonDigits)
    }

    @Test
    fun `category is required and errors accumulate`() {
        val errors = AddTransactionValidator.validate(null, PaymentMethod.CARD, "", null)
        assertEquals(
            setOf(Error.AMOUNT_REQUIRED, Error.CATEGORY_REQUIRED, Error.LAST4_INVALID),
            errors,
        )
    }
}
