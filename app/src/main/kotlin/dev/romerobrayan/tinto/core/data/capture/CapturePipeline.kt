package dev.romerobrayan.tinto.core.data.capture

import android.util.Log
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The single entry point every capture source feeds: parse → dedupe → stage.
 * Never writes to the ledger — a recognized item only ever lands in the
 * device-local pending store for explicit user review (CLAUDE.md guardrail:
 * never auto-commit a parse).
 */
@Singleton
class CapturePipeline @Inject constructor(
    private val parser: TransactionParser,
    private val pendingRepository: PendingTransactionRepository,
    private val cardRepository: CardRepository,
    private val analytics: TintoAnalytics,
) {

    suspend fun submit(raw: RawCapture) {
        // Cards enable the last4 → cardId auto-match. Signed-out or unavailable
        // simply means no match — the capture still stages.
        val cards = runCatching { cardRepository.observeCards().first() }.getOrDefault(emptyList())
        when (val result = parser.parse(raw, cards)) {
            is ParseResult.Recognized -> {
                val staged = pendingRepository.stage(result.pending, rawKey(raw))
                if (staged) {
                    // Coarse channel + issuer only — never amounts, merchants,
                    // senders or raw text (TASK_SPRINT_3_CAPTURE.md privacy rule).
                    analytics.logCaptureDetected(
                        channel = result.pending.channel.name,
                        issuer = result.pending.issuer,
                    )
                }
            }
            // Known noise and unmatched bodies drop silently by design; the
            // debug line (sender only, never the body) is the rule-tuning hook.
            is ParseResult.Ignored ->
                Log.d(TAG, "Dropped known-noise capture from ${raw.sender}: ${result.reason}")
            ParseResult.Unrecognized ->
                Log.d(TAG, "Dropped unrecognized capture from ${raw.sender}")
        }
    }

    /**
     * Dedupe identity of a capture: sender + body (bodies embed the issuer's
     * own timestamp, so identical text is the same event). receivedAt stays
     * out on purpose — the live broadcast and the inbox backfill see different
     * timestamps for the same message.
     */
    private fun rawKey(raw: RawCapture): String = sha256("${raw.sender}|${raw.body}")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { "%02x".format(it) }

    private companion object {
        const val TAG = "CapturePipeline"
    }
}
