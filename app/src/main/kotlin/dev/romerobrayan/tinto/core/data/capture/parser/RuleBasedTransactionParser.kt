package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.repository.ParseResult
import dev.romerobrayan.tinto.core.domain.repository.TransactionParser
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser core: pure Kotlin, driven entirely by the [IssuerRules] data. A
 * message stages only when a template yields both an amount and a direction;
 * anything else is dropped ([ParseResult.Ignored] / [ParseResult.Unrecognized])
 * — silent-drop is the default, a false detection is worse than a miss.
 */
@Singleton
class RuleBasedTransactionParser @Inject constructor() : TransactionParser {

    private val rulesBySender: Map<String, IssuerRule> =
        IssuerRules.all
            .flatMap { rule -> rule.senders.map { sender -> sender to rule } }
            .toMap()

    override fun parse(raw: RawCapture): ParseResult {
        // Verbatim first (notification package names), then digits-only (SMS
        // shortcodes arrive with carrier prefixes/formatting around them).
        val rule = rulesBySender[raw.sender]
            ?: raw.sender.filter(Char::isDigit).takeIf(String::isNotEmpty)?.let(rulesBySender::get)
            ?: return ParseResult.Unrecognized

        rule.dropPatterns.firstOrNull { it.regex.containsMatchIn(raw.body) }
            ?.let { return ParseResult.Ignored(it.reason) }

        rule.templates.forEach { template ->
            val match = template.regex.find(raw.body) ?: return@forEach
            val amount = match.groupOrNull("amount")
                ?.let { AmountParser.parse(it, rule.amountConvention) }
                ?: return@forEach
            return ParseResult.Recognized(
                PendingTransaction(
                    id = UUID.randomUUID().toString(),
                    channel = raw.channel,
                    issuer = rule.issuer,
                    rawBody = raw.body,
                    type = template.type,
                    amount = amount,
                    last4 = match.groupOrNull("last4"),
                    // Matched against the registered cards at staging time.
                    cardId = null,
                    bank = rule.bank,
                    merchant = match.groupOrNull("merchant")?.cleanMerchant(),
                    occurredAt = CaptureDateParser.parse(
                        dateText = match.groupOrNull("date"),
                        timeText = match.groupOrNull("time"),
                        receivedAt = raw.receivedAt,
                    ),
                    capturedAt = raw.receivedAt,
                ),
            )
        }
        return ParseResult.Unrecognized
    }

    /** Named-group read that tolerates templates not defining the group. */
    private fun MatchResult.groupOrNull(name: String): String? =
        runCatching { groups[name]?.value }.getOrNull()

    /** Redacted or empty counterparties ("...", "***") collapse to null. */
    private fun String.cleanMerchant(): String? =
        trim().takeIf { text -> text.any { it.isLetterOrDigit() } }
}
