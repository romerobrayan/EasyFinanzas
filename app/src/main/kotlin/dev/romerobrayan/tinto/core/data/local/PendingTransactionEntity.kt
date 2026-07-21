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
 * Room row for the capture staging store. Primitives only (cents, epoch
 * millis, enum names). Confirmed/discarded rows are kept — with a terminal
 * [status] — so their [dedupKey] keeps a re-backfill from resurrecting them.
 */
@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["dedupKey"], unique = true), Index(value = ["status"])],
)
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    /** Stable key of the source message; the same capture never stages twice. */
    val dedupKey: String,
    /** PENDING → in the inbox; CONFIRMED / DISCARDED → resolved, kept for dedup. */
    val status: String,
    val channel: String,
    val issuer: String,
    val rawBody: String,
    val type: String,
    val amountCents: Long,
    val last4: String?,
    val cardId: String?,
    val bank: String?,
    val merchant: String?,
    val occurredAtEpochMs: Long,
    val capturedAtEpochMs: Long,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_DISCARDED = "DISCARDED"

        fun dedupKeyOf(pending: PendingTransaction): String {
            // A notification storm re-posts the same content with a nudged
            // postTime; bucketing the timestamp keeps every re-emission on one
            // key. SMS keeps exact millis (unchanged for existing rows).
            val timeKey = when (pending.channel) {
                CaptureChannel.NOTIFICATION ->
                    "b${pending.occurredAt.toEpochMilliseconds() / NOTIFICATION_DEDUP_BUCKET_MS}"

                else -> pending.occurredAt.toEpochMilliseconds().toString()
            }
            return "${pending.issuer}|$timeKey|${pending.rawBody.hashCode()}"
        }

        private const val NOTIFICATION_DEDUP_BUCKET_MS = 10 * 60 * 1000L
    }
}

internal fun PendingTransaction.toEntity(): PendingTransactionEntity = PendingTransactionEntity(
    id = id,
    dedupKey = PendingTransactionEntity.dedupKeyOf(this),
    status = PendingTransactionEntity.STATUS_PENDING,
    channel = channel.name,
    issuer = issuer,
    rawBody = rawBody,
    type = type.name,
    amountCents = amount.cents,
    last4 = last4,
    cardId = cardId,
    bank = bank,
    merchant = merchant,
    occurredAtEpochMs = occurredAt.toEpochMilliseconds(),
    capturedAtEpochMs = capturedAt.toEpochMilliseconds(),
)

/** A malformed row maps to null and is skipped, mirroring the Firestore mappers. */
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
        occurredAt = Instant.fromEpochMilliseconds(occurredAtEpochMs),
        capturedAt = Instant.fromEpochMilliseconds(capturedAtEpochMs),
    )
}.getOrNull()
