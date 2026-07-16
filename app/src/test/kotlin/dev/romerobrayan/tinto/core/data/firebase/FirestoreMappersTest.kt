package dev.romerobrayan.tinto.core.data.firebase

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the Firestore document schema: field names and primitive encodings
 * (cents as Long, instants as epoch millis, dates as ISO strings) must stay
 * stable — documents already written with these keys live in user accounts.
 */
class FirestoreMappersTest {

    @Test
    fun `transaction encodes to primitive firestore fields`() {
        val occurredAt = Instant.fromEpochMilliseconds(1_752_681_600_000)
        val transaction = Transaction(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amount = Money.ofPesos(18_000),
            method = PaymentMethod.CARD,
            cardId = "card-nu",
            bank = "Nu",
            categoryId = "cat-comida",
            merchant = "Almuerzo",
            occurredAt = occurredAt,
            source = TransactionSource.MANUAL,
            createdAt = occurredAt,
            updatedAt = occurredAt,
        )

        val map = transaction.toFirestoreMap()

        assertEquals("EXPENSE", map["type"])
        assertEquals(1_800_000L, map["amountCents"])
        assertEquals("CARD", map["method"])
        assertEquals("card-nu", map["cardId"])
        assertEquals("Nu", map["bank"])
        assertEquals("cat-comida", map["categoryId"])
        assertEquals("Almuerzo", map["merchant"])
        assertEquals(1_752_681_600_000L, map["occurredAt"])
        assertEquals("MANUAL", map["source"])
        assertEquals(1_752_681_600_000L, map["createdAt"])
        assertEquals(1_752_681_600_000L, map["updatedAt"])
    }

    @Test
    fun `cash transaction keeps null card fields`() {
        val now = Instant.fromEpochMilliseconds(0)
        val map = Transaction(
            id = "tx-2",
            type = TransactionType.INCOME,
            amount = Money.Zero,
            method = PaymentMethod.CASH,
            cardId = null,
            bank = null,
            categoryId = "cat-otros",
            merchant = null,
            occurredAt = now,
            source = TransactionSource.MANUAL,
            createdAt = now,
            updatedAt = now,
        ).toFirestoreMap()

        assertNull(map["cardId"])
        assertNull(map["bank"])
        assertNull(map["merchant"])
    }

    @Test
    fun `card encodes to primitive firestore fields`() {
        val map = Card(
            id = "card-1",
            bank = "Bancolombia",
            last4 = "3092",
            label = "Débito",
        ).toFirestoreMap()

        assertEquals("Bancolombia", map["bank"])
        assertEquals("3092", map["last4"])
        assertEquals("Débito", map["label"])
    }

    @Test
    fun `card without label keeps the field null`() {
        val map = Card(id = "card-2", bank = "Nu", last4 = "2481", label = null).toFirestoreMap()

        assertNull(map["label"])
    }

    @Test
    fun `reminder encodes due date as iso string`() {
        val map = Reminder(
            id = "rem-1",
            title = "Arriendo",
            amount = Money.ofPesos(950_000),
            dueDate = LocalDate(2026, 8, 1),
            recurrence = Recurrence.MONTHLY,
            isPaid = false,
        ).toFirestoreMap()

        assertEquals("Arriendo", map["title"])
        assertEquals(95_000_000L, map["amountCents"])
        assertEquals("2026-08-01", map["dueDate"])
        assertEquals("MONTHLY", map["recurrence"])
        assertEquals(false, map["isPaid"])
    }
}
