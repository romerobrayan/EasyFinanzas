package dev.romerobrayan.tinto.core.data.export

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the versioned JSON export (`ARCHITECTURE.md` §"Export
 * contract"). Mapped from domain models, never serialized directly off Room
 * or Firestore types, so persistence changes never silently alter it.
 *
 * TODO(sprint-N): carry `recurring_rules` too. For a no-account profile this
 *  document is the only way data reaches another device, and automations
 *  currently don't travel — adding the array means bumping [schemaVersion]
 *  and teaching `DataImporter` the new version.
 */
@Serializable
data class ExportDocument(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("exported_at") val exportedAt: String,
    @SerialName("app_version") val appVersion: String,
    val currency: String = "COP",
    val cards: List<CardExportDto>,
    val categories: List<CategoryExportDto>,
    val transactions: List<TransactionExportDto>,
    val reminders: List<ReminderExportDto>,
)

@Serializable
data class CardExportDto(
    val id: String,
    val bank: String,
    val last4: String,
    val label: String? = null,
)

@Serializable
data class CategoryExportDto(
    val id: String,
    val name: String,
    @SerialName("icon_key") val iconKey: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("is_system") val isSystem: Boolean,
    val scope: String,
)

@Serializable
data class TransactionExportDto(
    val id: String,
    val type: String,
    @SerialName("amount_cents") val amountCents: Long,
    val method: String,
    @SerialName("card_id") val cardId: String? = null,
    val bank: String? = null,
    @SerialName("category_id") val categoryId: String,
    val merchant: String? = null,
    @SerialName("occurred_at") val occurredAt: String,
    val source: String,
)

@Serializable
data class ReminderExportDto(
    val id: String,
    val title: String,
    @SerialName("amount_cents") val amountCents: Long? = null,
    @SerialName("due_date") val dueDate: String,
    @SerialName("due_time") val dueTime: String? = null,
    val recurrence: String,
    @SerialName("is_paid") val isPaid: Boolean,
)

fun Card.toExportDto() = CardExportDto(id = id, bank = bank, last4 = last4, label = label)

fun Category.toExportDto() = CategoryExportDto(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isSystem = isSystem,
    scope = scope.name,
)

fun Transaction.toExportDto() = TransactionExportDto(
    id = id,
    type = type.name,
    amountCents = amount.cents,
    method = method.name,
    cardId = cardId,
    bank = bank,
    categoryId = categoryId,
    merchant = merchant,
    occurredAt = occurredAt.toString(),
    source = source.name,
)

fun Reminder.toExportDto() = ReminderExportDto(
    id = id,
    title = title,
    amountCents = amount?.cents,
    dueDate = dueDate.toString(),
    dueTime = dueTime?.toString(),
    recurrence = recurrence.name,
    isPaid = isPaid,
)

fun CardExportDto.toDomain() = Card(id = id, bank = bank, last4 = last4, label = label)

/** Tolerant like `FirestoreMappers`: a malformed row is skipped, not fatal to the whole import. */
fun TransactionExportDto.toDomain(): Transaction? = runCatching {
    val occurredAtParsed = Instant.parse(occurredAt)
    Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amountCents),
        method = PaymentMethod.valueOf(method),
        cardId = cardId,
        bank = bank,
        categoryId = categoryId,
        merchant = merchant,
        occurredAt = occurredAtParsed,
        // Export never carries createdAt/updatedAt (bookkeeping, not user data);
        // an imported row starts its bookkeeping clock at its own occurredAt.
        source = runCatching { TransactionSource.valueOf(source) }.getOrNull() ?: TransactionSource.MANUAL,
        createdAt = occurredAtParsed,
        updatedAt = occurredAtParsed,
    )
}.getOrNull()

fun ReminderExportDto.toDomain(): Reminder? = runCatching {
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
