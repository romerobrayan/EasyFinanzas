package dev.romerobrayan.tinto.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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

/*
 * The device-local ledger: what a no-account (UserSession.Local) user's data
 * is stored as. Kept in one file for the same reason FirestoreMappers is —
 * these column names and the mappers below are a single persisted schema, and
 * reading them together is how you keep them consistent.
 *
 * Same conventions as the Firestore side: Money as cents (INTEGER), Instant as
 * epoch millis, LocalDate/LocalTime as ISO text, enums as their names, and
 * tolerant mappers so one malformed row never takes down the whole list.
 *
 * Unlike pending_transactions these tables hold data the user typed and cannot
 * re-derive — every schema change from version 3 on needs a real migration,
 * not the destructive fallback (see DatabaseModule).
 */

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["occurredAtEpochMs"]), Index(value = ["categoryId"])],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val method: String,
    val cardId: String?,
    val bank: String?,
    val categoryId: String,
    val merchant: String?,
    val occurredAtEpochMs: Long,
    val source: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val bank: String,
    val last4: String,
    val label: String?,
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountCents: Long?,
    /** ISO-8601 date, e.g. `2026-08-05`. */
    val dueDate: String,
    /** ISO-8601 local time, e.g. `08:00`; null = date only. */
    val dueTime: String?,
    val recurrence: String,
    val isPaid: Boolean,
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val method: String,
    val cardId: String?,
    val bank: String?,
    val categoryId: String,
    val merchant: String?,
    val frequency: String,
    val anchorDate: String,
    val nextOccurrence: String,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

internal fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amountCents = amount.cents,
    method = method.name,
    cardId = cardId,
    bank = bank,
    categoryId = categoryId,
    merchant = merchant,
    occurredAtEpochMs = occurredAt.toEpochMilliseconds(),
    source = source.name,
    createdAtEpochMs = createdAt.toEpochMilliseconds(),
    updatedAtEpochMs = updatedAt.toEpochMilliseconds(),
)

internal fun TransactionEntity.toDomain(): Transaction? = runCatching {
    Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amountCents),
        method = PaymentMethod.valueOf(method),
        cardId = cardId,
        bank = bank,
        categoryId = categoryId,
        merchant = merchant,
        occurredAt = Instant.fromEpochMilliseconds(occurredAtEpochMs),
        // Tolerant like the Firestore mapper: a source added by a newer build
        // reads back as MANUAL instead of dropping the movement.
        source = runCatching { TransactionSource.valueOf(source) }.getOrNull()
            ?: TransactionSource.MANUAL,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMs),
    )
}.getOrNull()

internal fun Card.toEntity() = CardEntity(id = id, bank = bank, last4 = last4, label = label)

internal fun CardEntity.toDomain() = Card(id = id, bank = bank, last4 = last4, label = label)

internal fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    amountCents = amount?.cents,
    dueDate = dueDate.toString(),
    dueTime = dueTime?.toString(),
    recurrence = recurrence.name,
    isPaid = isPaid,
)

internal fun ReminderEntity.toDomain(): Reminder? = runCatching {
    Reminder(
        id = id,
        title = title,
        amount = amountCents?.let(::Money),
        dueDate = LocalDate.parse(dueDate),
        dueTime = dueTime?.let(LocalTime::parse),
        recurrence = Recurrence.valueOf(recurrence),
        isPaid = isPaid,
    )
}.getOrNull()

internal fun RecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    type = type.name,
    amountCents = amount.cents,
    method = method.name,
    cardId = cardId,
    bank = bank,
    categoryId = categoryId,
    merchant = merchant,
    frequency = frequency.name,
    anchorDate = anchorDate.toString(),
    nextOccurrence = nextOccurrence.toString(),
    isActive = isActive,
    createdAtEpochMs = createdAt.toEpochMilliseconds(),
    updatedAtEpochMs = updatedAt.toEpochMilliseconds(),
)

internal fun RecurringRuleEntity.toDomain(): RecurringRule? = runCatching {
    RecurringRule(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amountCents),
        method = PaymentMethod.valueOf(method),
        cardId = cardId,
        bank = bank,
        categoryId = categoryId,
        merchant = merchant,
        frequency = TransactionFrequency.valueOf(frequency),
        anchorDate = LocalDate.parse(anchorDate),
        nextOccurrence = LocalDate.parse(nextOccurrence),
        isActive = isActive,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMs),
    )
}.getOrNull()
