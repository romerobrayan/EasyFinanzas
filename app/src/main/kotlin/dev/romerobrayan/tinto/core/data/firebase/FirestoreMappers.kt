package dev.romerobrayan.tinto.core.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.CategoryScope
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

/**
 * Manual Firestore document mappers. Deliberately not `toObject()` reflection:
 * `Money` is a value class and the domain models have no no-arg constructors.
 * Documents are keyed by the domain `id`; money travels as integer cents and
 * instants as epoch millis. A malformed document maps to null and is skipped —
 * one corrupt row must never take the whole statement down.
 */

internal fun Transaction.toFirestoreMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "amountCents" to amount.cents,
    "method" to method.name,
    "cardId" to cardId,
    "bank" to bank,
    "categoryId" to categoryId,
    "merchant" to merchant,
    "occurredAt" to occurredAt.toEpochMilliseconds(),
    "source" to source.name,
    "createdAt" to createdAt.toEpochMilliseconds(),
    "updatedAt" to updatedAt.toEpochMilliseconds(),
)

internal fun DocumentSnapshot.toTransaction(): Transaction? = runCatching {
    val occurredAt = Instant.fromEpochMilliseconds(getLong("occurredAt") ?: return null)
    Transaction(
        id = id,
        type = TransactionType.valueOf(getString("type") ?: return null),
        amount = Money(getLong("amountCents") ?: return null),
        method = PaymentMethod.valueOf(getString("method") ?: return null),
        cardId = getString("cardId"),
        bank = getString("bank"),
        categoryId = getString("categoryId") ?: return null,
        merchant = getString("merchant"),
        occurredAt = occurredAt,
        // Tolerant: an unknown/newer source degrades to MANUAL rather than
        // dropping the whole movement.
        source = getString("source")?.let { runCatching { TransactionSource.valueOf(it) }.getOrNull() }
            ?: TransactionSource.MANUAL,
        createdAt = getLong("createdAt")?.let(Instant::fromEpochMilliseconds) ?: occurredAt,
        updatedAt = getLong("updatedAt")?.let(Instant::fromEpochMilliseconds) ?: occurredAt,
    )
}.getOrNull()

internal fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "iconKey" to iconKey,
    "colorHex" to colorHex,
    "isSystem" to isSystem,
    "scope" to scope.name,
)

internal fun DocumentSnapshot.toCategory(): Category? = runCatching {
    Category(
        id = id,
        name = getString("name") ?: return null,
        iconKey = getString("iconKey") ?: "dots",
        colorHex = getString("colorHex") ?: "#B99CA6",
        isSystem = getBoolean("isSystem") ?: false,
        // Additive field: rows written before Sprint 5 have no "scope" → expense.
        scope = getString("scope")?.let { runCatching { CategoryScope.valueOf(it) }.getOrNull() }
            ?: CategoryScope.EXPENSE,
    )
}.getOrNull()

internal fun RecurringRule.toFirestoreMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "amountCents" to amount.cents,
    "method" to method.name,
    "cardId" to cardId,
    "bank" to bank,
    "categoryId" to categoryId,
    "merchant" to merchant,
    "frequency" to frequency.name,
    "anchorDate" to anchorDate.toString(),
    "nextOccurrence" to nextOccurrence.toString(),
    "isActive" to isActive,
    "createdAt" to createdAt.toEpochMilliseconds(),
    "updatedAt" to updatedAt.toEpochMilliseconds(),
)

internal fun DocumentSnapshot.toRecurringRule(): RecurringRule? = runCatching {
    val createdAt = getLong("createdAt")?.let(Instant::fromEpochMilliseconds) ?: return null
    RecurringRule(
        id = id,
        // Unknown enum values fall back safely so one odd row never crashes.
        type = getString("type")?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            ?: return null,
        amount = Money(getLong("amountCents") ?: return null),
        method = getString("method")?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() }
            ?: PaymentMethod.CASH,
        cardId = getString("cardId"),
        bank = getString("bank"),
        categoryId = getString("categoryId") ?: return null,
        merchant = getString("merchant"),
        frequency = getString("frequency")?.let { runCatching { TransactionFrequency.valueOf(it) }.getOrNull() }
            ?: return null,
        anchorDate = LocalDate.parse(getString("anchorDate") ?: return null),
        nextOccurrence = LocalDate.parse(getString("nextOccurrence") ?: return null),
        isActive = getBoolean("isActive") ?: true,
        createdAt = createdAt,
        updatedAt = getLong("updatedAt")?.let(Instant::fromEpochMilliseconds) ?: createdAt,
    )
}.getOrNull()

internal fun Card.toFirestoreMap(): Map<String, Any?> = mapOf(
    "bank" to bank,
    "last4" to last4,
    "label" to label,
)

internal fun DocumentSnapshot.toCard(): Card? = runCatching {
    Card(
        id = id,
        bank = getString("bank") ?: return null,
        last4 = getString("last4") ?: return null,
        label = getString("label"),
    )
}.getOrNull()

internal fun Reminder.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "amountCents" to amount?.cents,
    "dueDate" to dueDate.toString(),
    "dueTime" to dueTime?.toString(),
    "recurrence" to recurrence.name,
    "isPaid" to isPaid,
)

internal fun DocumentSnapshot.toReminder(): Reminder? = runCatching {
    Reminder(
        id = id,
        title = getString("title") ?: return null,
        amount = getLong("amountCents")?.let(::Money),
        dueDate = LocalDate.parse(getString("dueDate") ?: return null),
        // Tolerant on purpose: a malformed time degrades to date-only
        // instead of dropping the whole reminder.
        dueTime = getString("dueTime")?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        recurrence = getString("recurrence")?.let(Recurrence::valueOf) ?: Recurrence.NONE,
        isPaid = getBoolean("isPaid") ?: false,
    )
}.getOrNull()
