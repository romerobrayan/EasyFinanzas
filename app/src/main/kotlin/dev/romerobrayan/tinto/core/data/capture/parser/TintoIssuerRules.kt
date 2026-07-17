package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * The live rule sets, straight from the field reference in
 * TASK_SPRINT_3_CAPTURE.md. Nu (push notification, Colombian separators,
 * relative dates → receivedAt fallback) is documented there as the sprint-4
 * fast-follow; its rule object slots in right here when the notification
 * source goes live.
 */
// TODO(sprint-4): add the Nu notification rule set (senders = package name).
object TintoIssuerRules {

    /**
     * Bancolombia, SMS shortcode 85540. US-style amounts, day-first dates,
     * account masks `*5005` / `**5005` / `producto 5005` / `cuenta 5005`.
     */
    val bancolombia = IssuerRule(
        issuer = "Bancolombia",
        senders = setOf("85540"),
        decimalConvention = DecimalConvention.DOT_DECIMAL,
        dropPatterns = emptyList(),
        templates = listOf(
            // "BRAYAN ROMERO DORADO pagaste $18,000.00 por codigo QR desde tu
            // cuenta *5005 a la llave 0089378571 el ..."
            MessageTemplate(
                id = "pago-qr",
                pattern = Regex("""(?i)\bpagaste\s+\$[\d.,]+\s+por\s+codigo\s+QR\b.*?\ba\s+la\s+llave\s+(\S+)"""),
                type = TransactionType.EXPENSE,
                merchant = { it.groupValues[1] },
            ),
            // Bre-b, no leading verb: "$15,000.00 a la llave @samueld7650
            // desde tu cuenta *5005 a samuel diaz el 07/07/26 a las 14:45."
            MessageTemplate(
                id = "pago-breb",
                pattern = Regex("""\$[\d.,]+\s+a\s+la\s+llave\s+@\S+\s+desde\s+tu\s+cuenta\s+\S+\s+a\s+(.+?)\s+el\s"""),
                type = TransactionType.EXPENSE,
                merchant = { it.groupValues[1] },
            ),
            // "Pagaste $152,372.00 a 101 FINTECH S A S desde tu producto 5005 ..."
            MessageTemplate(
                id = "pago",
                pattern = Regex("""(?i)\bpagaste\s+\$[\d.,]+\s+a\s+(.+?)\s+desde\s+tu\s+(?:cuenta|producto)\b"""),
                type = TransactionType.EXPENSE,
                merchant = { it.groupValues[1] },
            ),
            // "Transferiste $150,000.00 desde tu cuenta 5005 a la cuenta *3105687364 ..."
            MessageTemplate(
                id = "transferencia-enviada",
                pattern = Regex("""(?i)\btransferiste\s+\$[\d.,]+\b.*?\ba\s+la\s+cuenta\s+(\*?\d+)"""),
                type = TransactionType.EXPENSE,
                merchant = { "cuenta ${it.groupValues[1]}" },
            ),
            // "Recibiste una transferencia por $10,000 de ANDRES OSORNO en tu cuenta **5005 ..."
            MessageTemplate(
                id = "transferencia-recibida",
                pattern = Regex("""(?i)\brecibiste\s+una\s+transferencia\s+por\s+\$[\d.,]+\s+de\s+(.+?)\s+en\s+tu\s+cuenta\b"""),
                type = TransactionType.INCOME,
                merchant = { it.groupValues[1] },
            ),
            // "Recibiste una consignacion por $1,487,941 desde el corresponsal
            // MULTIPAGAS UNICENTRO CENTRO CO en MEDELLIN, el 15/07/26 16:56."
            // The optional " en <CITY>" tail keeps the city out of the merchant.
            MessageTemplate(
                id = "consignacion",
                pattern = Regex("""(?i)\brecibiste\s+una\s+consignacion\s+por\s+\$[\d.,]+\s+desde\s+el\s+corresponsal\s+(.+?)(?:\s+en\s+[^,]+)?,"""),
                type = TransactionType.INCOME,
                merchant = { it.groupValues[1] },
            ),
        ),
        dateLayouts = listOf(DateLayouts.dayMonthYear),
        // Source-anchored ("desde/en tu ...") so a transfer's destination
        // account is never mistaken for the user's own product.
        accountMaskPatterns = listOf(
            Regex("""(?i)(?:desde|en)\s+tu\s+(?:cuenta|producto)\s+\*{0,2}(\d{4,})"""),
        ),
    )

    /**
     * 1CERO1 (the work credit card), SMS shortcode 891134. Colombian-style
     * amounts, year-first dates, card mask `TC 49846720*****5427`.
     */
    val unoCeroUno = IssuerRule(
        issuer = "1CERO1",
        senders = setOf("891134"),
        decimalConvention = DecimalConvention.COMMA_DECIMAL,
        dropPatterns = listOf(
            // PIN change: no amount, not a movement.
            Regex("""(?i)\brealizo\s+Cambio\s+Clave\b"""),
        ),
        templates = listOf(
            // "1CERO1 informa realizo Compra MERCADO PAGO*MERCADOL, el
            // 2026/06/25 a las 11:16:59 por $851.791,00 con la TC ...5427 ."
            MessageTemplate(
                id = "compra-tc",
                pattern = Regex("""(?i)\binforma\s+realizo\s+Compra\s*(.*?),\s*el\s"""),
                type = TransactionType.EXPENSE,
                merchant = { it.groupValues[1] },
            ),
            // Card-bill payment; its cash counterpart is a Bancolombia
            // "Pagaste" for the same amount/minute — the prime duplicate case.
            // Staged anyway; the inbox flags the pair.
            MessageTemplate(
                id = "pago-tc",
                pattern = Regex("""(?i)\binforma\s+realizo\s+Pago\s*,"""),
                type = TransactionType.EXPENSE,
            ),
        ),
        dateLayouts = listOf(DateLayouts.yearMonthDay),
        accountMaskPatterns = listOf(
            Regex("""(?i)\bcon\s+la\s+TC\s+\d*\*+(\d{4})"""),
        ),
    )

    val all: List<IssuerRule> = listOf(bancolombia, unoCeroUno)

    /** The capture sources' sender allowlist — derived from the rules, one source of truth. */
    fun isAllowlisted(sender: String): Boolean = all.any { it.matchesSender(sender) }

    fun ruleFor(sender: String): IssuerRule? = all.firstOrNull { it.matchesSender(sender) }
}
