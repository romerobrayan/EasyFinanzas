package dev.romerobrayan.tinto.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.Instant

/**
 * Room row for a staged capture. Primitives only: money as integer cents,
 * instants as epoch millis, enums as their names — mirroring the Firestore
 * mapper conventions. The raw SMS body is kept solely for the review sheet
 * and rule tuning; this table is device-local and never synced.
 */
@Entity(tableName = "pending_transactions", indices = [Index("occurredAt")])
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    val channel: String,
    val issuer: String,
    val rawBody: String,
    val type: String,
    val amountCents: Long,
    val last4: String?,
    val cardId: String?,
    val bank: String?,
    val merchant: String?,
    val occurredAt: Long,
    val capturedAt: Long,
)

internal fun PendingTransaction.toEntity(): PendingTransactionEntity = PendingTransactionEntity(
    id = id,
    channel = channel.name,
    issuer = issuer,
    rawBody = rawBody,
    type = type.name,
    amountCents = amount.cents,
    last4 = last4,
    cardId = cardId,
    bank = bank,
    merchant = merchant,
    occurredAt = occurredAt.toEpochMilliseconds(),
    capturedAt = capturedAt.toEpochMilliseconds(),
)

/** A malformed row maps to null and is skipped — never takes the inbox down. */
internal fun PendingTransactionEntity.toDomain(): PendingTransaction? = runCatching {
    PendingTransaction(
        id = id,
        channel = CaptureChannel.valueOf(channel),
        issuer = issuer,
        rawBody = rawBody,
        type = TransactionType.valueOf(type),
        amount = Money(amountCents),
        last4 = last4,
        cardId = cardId,
        bank = bank,
        merchant = merchant,
        occurredAt = Instant.fromEpochMilliseconds(occurredAt),
        capturedAt = Instant.fromEpochMilliseconds(capturedAt),
    )
}.getOrNull()
