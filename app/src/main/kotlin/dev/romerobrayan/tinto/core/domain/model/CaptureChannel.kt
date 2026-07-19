package dev.romerobrayan.tinto.core.domain.model

/**
 * Where a raw bank message came from. SMS (Bancolombia/1CERO1) and
 * NOTIFICATION (Nu push) are both live channels.
 */
enum class CaptureChannel { SMS, NOTIFICATION }

/** Provenance carried into the ledger on confirm — never rewritten to MANUAL. */
fun CaptureChannel.asTransactionSource(): TransactionSource = when (this) {
    CaptureChannel.SMS -> TransactionSource.SMS
    CaptureChannel.NOTIFICATION -> TransactionSource.NOTIFICATION
}
