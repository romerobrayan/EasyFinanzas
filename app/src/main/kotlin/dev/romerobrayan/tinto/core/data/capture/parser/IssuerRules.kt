package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * The issuer rule sets — data, not code branches. Sources of truth:
 * TASK_SPRINT_3_CAPTURE.md "Message formats". Adding a bank is adding an
 * [IssuerRule] here (senders + templates); the parser core stays untouched.
 *
 * Regex building blocks: `amount` is always required; `merchant`, `last4`,
 * `date` and `time` are optional named groups read uniformly by the core.
 */
object IssuerRules {

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

    // TODO(sprint-4): Nu push-notification rule set (Colombian separators,
    // relative dates → RawCapture.receivedAt, "terminada en NNNN" mask, and the
    // "Tienes un pago" request as a drop pattern). Formats documented in
    // TASK_SPRINT_3_CAPTURE.md.

    /** Every active rule; the parser core indexes them by sender. */
    val all: List<IssuerRule> = listOf(bancolombia, oneCero1)

    /** Digits-only sender allowlist for the backfill query filter. */
    val allSenders: Set<String> = all.flatMapTo(mutableSetOf()) { it.senders }
}
