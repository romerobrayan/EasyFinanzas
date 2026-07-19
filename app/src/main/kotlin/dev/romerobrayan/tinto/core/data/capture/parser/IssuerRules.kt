package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * The issuer rule sets — data, not code branches. Sources of truth:
 * TASK_SPRINT_3_CAPTURE.md "Message formats" and the Sprint 4 appendix.
 * Adding a bank is adding an [IssuerRule] here (senders + templates); the
 * parser core stays untouched.
 *
 * Regex building blocks: `amount` is always required; `merchant`, `last4`,
 * `date` and `time` are optional named groups read uniformly by the core.
 */
object IssuerRules {

    /**
     * The Nu app's package name — the notification-channel equivalent of an
     * SMS shortcode. Single point of change for the allowlist.
     */
    // TODO: confirmar package real de Nu en el dispositivo (valor provisional).
    const val NU_PACKAGE_NAME: String = "com.nu.production"

    private const val AMOUNT = """\$(?<amount>[\d.,]+)"""
    private const val DATE = """(?<date>\d{1,2}/\d{1,2}/\d{2,4}|\d{4}/\d{1,2}/\d{1,2})"""
    private const val TIME = """(?<time>\d{1,2}:\d{2}(?::\d{2})?)"""
    private const val DATE_TIME = """el $DATE(?:,?\s+a\s+las)?\s+$TIME"""

    /** Bancolombia SMS (sender 85540): US-style separators, DD/MM dates. */
    val bancolombia: IssuerRule = IssuerRule(
        issuer = "Bancolombia",
        bank = "Bancolombia",
        senders = setOf("85540"),
        amountConvention = AmountConvention.DOT_DECIMAL,
        templates = listOf(
            // "pagaste $X por codigo QR desde tu cuenta *5005 a la llave K el …"
            MessageTemplate(
                regex = Regex(
                    """pagaste $AMOUNT por codigo QR desde tu cuenta \*{0,2}(?<last4>\d{4}) a la llave (?<merchant>\S+) $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "$X a la llave @K desde tu cuenta *5005 a <name> el …" (Bre-b)
            MessageTemplate(
                regex = Regex(
                    """$AMOUNT a la llave \S+ desde tu cuenta \*{0,2}(?<last4>\d{4}) a (?<merchant>.+?) $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "Pagaste $X a <NEG> desde tu producto/cuenta 5005 el …"
            MessageTemplate(
                regex = Regex(
                    """pagaste $AMOUNT a (?<merchant>.+?) desde tu (?:producto|cuenta) \*{0,2}(?<last4>\d{4}) $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "Transferiste $X desde tu cuenta 5005 a la cuenta *N el …"
            MessageTemplate(
                regex = Regex(
                    """transferiste $AMOUNT desde tu cuenta \*{0,2}(?<last4>\d{4}) a la cuenta (?<merchant>\*?\d+) $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "Recibiste una transferencia por $X de <name> en tu cuenta **5005, el …"
            MessageTemplate(
                regex = Regex(
                    """recibiste una transferencia por $AMOUNT de (?<merchant>.+?) en tu cuenta \*{0,2}(?<last4>\d{4}),? $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.INCOME,
            ),
            // "Recibiste una consignacion por $X desde el corresponsal <name>, el …"
            MessageTemplate(
                regex = Regex(
                    """recibiste una consignacion por $AMOUNT desde el corresponsal (?<merchant>.+?),? $DATE_TIME""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.INCOME,
            ),
        ),
    )

    /** 1CERO1 SMS (sender 891134): Colombian separators, YYYY/MM/DD dates. */
    val oneCero1: IssuerRule = IssuerRule(
        issuer = "1CERO1",
        bank = "101Fintech",
        senders = setOf("891134"),
        amountConvention = AmountConvention.COMMA_DECIMAL,
        templates = listOf(
            // "1CERO1 informa realizo Compra <MERCHANT>, el YYYY/MM/DD a las HH:mm:ss
            //  por $X con la TC 49846720*****5427". "Pago" is the credit-card bill
            // payment — staged as an expense by decision; the duplicate warning
            // reconciles it against the Bancolombia "Pagaste" counterpart.
            MessageTemplate(
                regex = Regex(
                    """realizo (?:Compra|Pago)\s*(?<merchant>[^,]*), $DATE_TIME por $AMOUNT con la TC [\d*]*(?<last4>\d{4})""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
        ),
        dropPatterns = listOf(
            DropPattern(
                regex = Regex("""realizo Cambio Clave""", RegexOption.IGNORE_CASE),
                reason = "cambio de clave (no es un movimiento)",
            ),
        ),
    )

    /**
     * Nu push notifications (matched by app package): Colombian separators,
     * relative dates only — templates carry no `date`/`time` groups, so the
     * parser falls back to `RawCapture.receivedAt` (the notification postTime).
     */
    val nu: IssuerRule = IssuerRule(
        issuer = "Nu",
        bank = "NU Bank",
        senders = setOf(NU_PACKAGE_NAME),
        amountConvention = AmountConvention.COMMA_DECIMAL,
        templates = listOf(
            // "Compra aprobada por $X - Tu compra en GOOGLE YouTube por $X con
            //  tu tarjeta terminada en 3101 ha sido APROBADA."
            MessageTemplate(
                regex = Regex(
                    """compra aprobada por $AMOUNT.*?tu compra en (?<merchant>.+?) por \$[\d.,]+ con tu tarjeta terminada en (?<last4>\d{4})""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "Pago aprobado por $X - Pagaste en GLOBAL COLOMBIA 81 SA con tu
            //  cuenta de ahorros." — no digits in the mask → no card match.
            MessageTemplate(
                regex = Regex(
                    """pago aprobado por $AMOUNT\s*-\s*pagaste en (?<merchant>.+?) con tu""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
            // "¡Bravo! Pagaste tu tarjeta de crédito Nu. Recibimos tu pago por
            //  $X." — CC bill payment; the duplicate warning reconciles it
            // against the Bancolombia "Pagaste a NU" counterpart.
            MessageTemplate(
                regex = Regex(
                    """pagaste tu tarjeta de cr[eé]dito Nu\.?\s*recibimos tu pago por $AMOUNT""",
                    RegexOption.IGNORE_CASE,
                ),
                type = TransactionType.EXPENSE,
            ),
        ),
        dropPatterns = listOf(
            // A payment *request* / pre-auth, not a completed movement (the
            // amount can even differ from the approved charge).
            DropPattern(
                regex = Regex("""tienes un pago por""", RegexOption.IGNORE_CASE),
                reason = "solicitud de pago (no es un movimiento completado)",
            ),
        ),
    )

    /** Every active rule; the parser core indexes them by sender. */
    val all: List<IssuerRule> = listOf(bancolombia, oneCero1, nu)

    /** Digits-only sender allowlist for the SMS backfill query filter. */
    val allSenders: Set<String> =
        all.flatMap { it.senders }.filterTo(mutableSetOf()) { sender -> sender.all(Char::isDigit) }
}
