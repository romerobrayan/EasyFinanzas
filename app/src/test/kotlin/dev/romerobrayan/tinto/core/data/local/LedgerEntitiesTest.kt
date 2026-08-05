package dev.romerobrayan.tinto.core.data.local

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The device-local ledger is the only copy a no-account user has, so these
 * mappers are the contract that keeps it readable: round-trips must be exact
 * (money to the centavo, instants to the millisecond) and a row a future build
 * wrote must degrade instead of taking the whole list down with it.
 */
class LedgerEntitiesTest {

    private val occurredAt = Instant.parse("2026-08-05T14:30:00Z")

    private val transaction = Transaction(
        id = "tx-1",
        type = TransactionType.EXPENSE,
        amount = Money(45_900_00),
        method = PaymentMethod.CARD,
        cardId = "card-1",
        bank = "Bancolombia",
        categoryId = "cat-mercado",
        merchant = "Éxito",
        occurredAt = occurredAt,
        source = TransactionSource.SMS,
        createdAt = occurredAt,
        updatedAt = Instant.parse("2026-08-06T09:00:00Z"),
    )

    @Test
    fun `transaction round-trips through the entity`() {
        assertEquals(transaction, transaction.toEntity().toDomain())
    }

    @Test
    fun `cash transaction keeps its null card and bank`() {
        val cash = transaction.copy(method = PaymentMethod.CASH, cardId = null, bank = null)

        assertEquals(cash, cash.toEntity().toDomain())
    }

    @Test
    fun `a source this build does not know reads back as manual`() {
        val row = transaction.toEntity().copy(source = "TELEPATHY")

        assertEquals(TransactionSource.MANUAL, row.toDomain()?.source)
    }

    @Test
    fun `a corrupt enum drops the row instead of the list`() {
        val row = transaction.toEntity().copy(type = "NOT_A_TYPE")

        assertNull(row.toDomain())
    }

    @Test
    fun `card round-trips through the entity`() {
        val card = Card(id = "card-1", bank = "NU Bank", last4 = "4821", label = "Personal")

        assertEquals(card, card.toEntity().toDomain())
    }

    @Test
    fun `reminder round-trips with its optional amount and time`() {
        val reminder = Reminder(
            id = "rem-1",
            title = "Arriendo",
            amount = Money(1_200_000_00),
            dueDate = LocalDate(2026, 8, 30),
            dueTime = LocalTime(8, 0),
            recurrence = Recurrence.MONTHLY,
            isPaid = false,
        )

        assertEquals(reminder, reminder.toEntity().toDomain())
        val dateOnly = reminder.copy(amount = null, dueTime = null)
        assertEquals(dateOnly, dateOnly.toEntity().toDomain())
    }

    @Test
    fun `recurring rule round-trips through the entity`() {
        val rule = RecurringRule(
            id = "rule-1",
            type = TransactionType.INCOME,
            amount = Money(3_500_000_00),
            method = PaymentMethod.TRANSFER,
            cardId = null,
            bank = null,
            categoryId = "cat-nomina",
            merchant = "Nómina",
            frequency = TransactionFrequency.SEMIMONTHLY,
            anchorDate = LocalDate(2026, 7, 15),
            nextOccurrence = LocalDate(2026, 8, 15),
            isActive = true,
            createdAt = occurredAt,
            updatedAt = occurredAt,
        )

        assertEquals(rule, rule.toEntity().toDomain())
    }
}
