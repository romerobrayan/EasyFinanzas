package dev.romerobrayan.tinto.core.domain.model

/**
 * Where a raw bank message came from. SMS is the only live channel this
 * sprint; NOTIFICATION is the scaffolded seam for the Nu push capture.
 */
enum class CaptureChannel { SMS, NOTIFICATION }

/** Provenance carried into the ledger on confirm — never rewritten to MANUAL. */
fun CaptureChannel.asTransactionSource(): TransactionSource = when (this) {
    CaptureChannel.SMS -> TransactionSource.SMS
    CaptureChannel.NOTIFICATION -> TransactionSource.NOTIFICATION
}
