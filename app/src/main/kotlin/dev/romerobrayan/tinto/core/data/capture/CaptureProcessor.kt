package dev.romerobrayan.tinto.core.data.capture

import android.util.Log
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The single path from a raw message into the staging store: parse → match
 * the mask against the registered cards → stage. Drops stay silent (debug
 * log only, for rule tuning) and nothing here ever writes to the ledger.
 */
@Singleton
class CaptureProcessor @Inject constructor(
    private val parser: TransactionParser,
    private val pendingRepository: PendingTransactionRepository,
    private val cardRepository: CardRepository,
    private val analytics: TintoAnalytics,
) {

    suspend fun process(raw: RawCapture) {
        when (val result = parser.parse(raw)) {
            is ParseResult.Recognized -> {
                val matchedCard = result.pending.last4?.let { last4 ->
                    cardRepository.observeCards().first().firstOrNull { it.last4 == last4 }
                }
                pendingRepository.stage(result.pending.copy(cardId = matchedCard?.id))
                // Channel + issuer only — never amounts, merchants or raw text.
                analytics.logCaptureDetected(raw.channel.name, result.pending.issuer)
            }

            is ParseResult.Ignored ->
                Log.d(TAG, "Dropped known-noise capture: ${result.reason}")

            ParseResult.Unrecognized ->
                Log.d(TAG, "Unrecognized capture from ${raw.sender} (${raw.body.length} chars)")
        }
    }

    private companion object {
        const val TAG = "CaptureProcessor"
    }
}
