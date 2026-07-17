package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import java.util.UUID

/**
 * The rule-set parser: pure Kotlin, no Android imports, JVM unit-testable.
 * Sender → issuer rule → drop patterns → ordered templates. A capture is
 * recognized only when a template match also yields an amount — silent drop
 * is the default for everything else.
 */
class RuleBasedTransactionParser(
    private val rules: List<IssuerRule>,
) : TransactionParser {

    override fun parse(raw: RawCapture, registeredCards: List<Card>): ParseResult {
        val rule = rules.firstOrNull { it.matchesSender(raw.sender) }
            ?: return ParseResult.Unrecognized

        rule.dropPatterns.firstOrNull { it.containsMatchIn(raw.body) }?.let {
            return ParseResult.Ignored(reason = "${rule.issuer}: known noise")
        }

        for (template in rule.templates) {
            val match = template.pattern.find(raw.body) ?: continue
            // A template hit without a parseable amount must not stage —
            // amount + direction are both required for a capture to exist.
            val amount = AmountParser.findAmount(raw.body, rule.decimalConvention)
                ?: return ParseResult.Unrecognized
            val occurredAt = rule.dateLayouts.firstNotNullOfOrNull { it.findIn(raw.body) }
                ?: raw.receivedAt
            val last4 = rule.accountMaskPatterns
                .firstNotNullOfOrNull { it.find(raw.body)?.groupValues?.get(1) }
                ?.takeLast(4)
            val matchedCard = last4?.let { digits -> registeredCards.firstOrNull { it.last4 == digits } }

            return ParseResult.Recognized(
                PendingTransaction(
                    id = UUID.randomUUID().toString(),
                    channel = raw.channel,
                    issuer = rule.issuer,
                    rawBody = raw.body,
                    type = template.type,
                    amount = amount,
                    last4 = last4,
                    cardId = matchedCard?.id,
                    bank = matchedCard?.bank ?: rule.issuer,
                    merchant = normalizeMerchant(template.merchant(match)),
                    occurredAt = occurredAt,
                    capturedAt = raw.receivedAt,
                ),
            )
        }
        return ParseResult.Unrecognized
    }

    /** Redacted or empty counterparties ("...", "") collapse to null. */
    private fun normalizeMerchant(merchant: String?): String? =
        merchant?.trim()?.takeIf { it.any(Char::isLetterOrDigit) }
}
