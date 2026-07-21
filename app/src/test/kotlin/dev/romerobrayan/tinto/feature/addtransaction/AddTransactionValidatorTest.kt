package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionValidator.Error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionValidatorTest {

    @Test
    fun `valid card expense passes`() {
        val errors = AddTransactionValidator.validate(
            amountPesos = 25_000,
            type = TransactionType.EXPENSE,
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
            type = TransactionType.EXPENSE,
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
            AddTransactionValidator.validate(0, TransactionType.EXPENSE, PaymentMethod.CASH, "", "comida"),
        )
        assertEquals(
            setOf(Error.AMOUNT_REQUIRED),
            AddTransactionValidator.validate(null, TransactionType.EXPENSE, PaymentMethod.CASH, "", "comida"),
        )
    }

    @Test
    fun `card expense requires exactly four digits`() {
        val tooShort = AddTransactionValidator.validate(
            5_000, TransactionType.EXPENSE, PaymentMethod.CARD, "309", "comida",
        )
        val nonDigits = AddTransactionValidator.validate(
            5_000, TransactionType.EXPENSE, PaymentMethod.CARD, "30a2", "comida",
        )
        assertEquals(setOf(Error.LAST4_INVALID), tooShort)
        assertEquals(setOf(Error.LAST4_INVALID), nonDigits)
    }

    @Test
    fun `category is required and errors accumulate`() {
        val errors = AddTransactionValidator.validate(
            null, TransactionType.EXPENSE, PaymentMethod.CARD, "", null,
        )
        assertEquals(
            setOf(Error.AMOUNT_REQUIRED, Error.CATEGORY_REQUIRED, Error.LAST4_INVALID),
            errors,
        )
    }

    @Test
    fun `income card does not require a manual last4`() {
        // Income picks a registered card — last4 comes pre-filled, the manual
        // field never shows, so an empty last4 must not error.
        val errors = AddTransactionValidator.validate(
            amountPesos = 2_850_000,
            type = TransactionType.INCOME,
            method = PaymentMethod.CARD,
            last4 = "",
            categoryId = "cat-nomina",
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `income transfer is valid without last4`() {
        val errors = AddTransactionValidator.validate(
            amountPesos = 500_000,
            type = TransactionType.INCOME,
            method = PaymentMethod.TRANSFER,
            last4 = "",
            categoryId = "cat-movimiento",
        )
        assertTrue(errors.isEmpty())
    }
}
