# TASK_SPRINT_3_CAPTURE.md — Sprint 3: automatic capture (SMS first)

## Where the app stands (context for this sprint)

Sprints 1 (UI shell), 1.5 (Firebase) and 2 (real-account CRUD) are done and
live on `main`:

- Session gate in `navigation/TintoRoot.kt`: `Loading → LoginScreen → Demo |
  SignedIn`. Data routes by session through the `Synced*Repository` classes in
  `core/data/repository/` — Cloud Firestore under `users/{uid}/…` when signed
  in, the `InMemory*` sample repositories in demo mode.
- The ledger supports full manual CRUD: add / edit / delete movements, card
  management in Perfil, reminders with recurrence rollover.
- Domain already models capture provenance:
  `TransactionSource { MANUAL, NOTIFICATION, EMAIL, SMS }`, and `Card.last4`
  stores **four** digits precisely so a captured movement can auto-match a
  registered card.

The gap: **every movement is still typed by hand.** The differentiating
feature of this app — reading bank messages and turning them into ledger
entries — does not exist yet. Sprint 3 builds it, **SMS first**, because the
user's real traffic (Bancolombia and the 1CERO1 work card) arrives as SMS.

> This is the planning brief the roadmap promised ("message-filtering rules are
> designed here"). It fixes the parser design, the issuer formats, and the
> pending-inbox UX before code is written. Read `ARCHITECTURE.md` §"Data-capture
> pipeline" and `DESIGN_SYSTEM.md` §"Voice / microcopy" alongside it.

## Decisions taken (planning)

| Decision | Choice | Consequence |
|---|---|---|
| First capture channel | **SMS** (`READ_SMS` + `RECEIVE_SMS`) | Covers Bancolombia + 1CERO1, the bulk of real messages. Notifications (Nu) are a scaffolded fast-follow, not implemented this sprint. |
| Direction | **Expenses + income** | Parser classifies debit vs credit per message. Incoming transfers/consignaciones stage as `INCOME`; everything outgoing as `EXPENSE`. |
| Transfers & card-bill payments | **Capture as expenses** | No transfer-detection ruleset this sprint. The same money seen twice (e.g. `$357.509` from Nu and Bancolombia) is surfaced by the **duplicate warning** in the inbox; the user discards one. |

Everything a parse produces lands in a **pending inbox** for user review — a
bad parse must never silently reach the ledger (`CLAUDE.md` guardrail).

## Scope at a glance

**In:** SMS capture source (live receive + one-time backfill) · a rule-set
parser for Bancolombia and 1CERO1 · a device-local staging store · a
pending-inbox review screen · the permission/onboarding flow · unit tests over
the real sample messages.

**Out (do not pull forward):** `NotificationListenerService` implementation
(scaffold the seam only), Gmail (Sprint 4), auto-categorization / ML, recurring
subscription detection, transfer auto-merge, any export or Firestore schema
change. Leave `// TODO(sprint-4):` markers where the notification/Gmail seams
are stubbed.

---

## Message formats — the field reference

This is the source of truth for the parser rule sets. Amounts are **not** in a
single locale: Bancolombia writes US-style separators, 1CERO1 and Nu write
Colombian-style. The decimal convention is therefore a property of each issuer
rule, not a global setting.

### Bancolombia — SMS, sender `85540`

- **Amounts:** US style — `,` groups, `.` decimals, and decimals are often
  omitted: `$15,000.00`, `$10,000`, `$1,487,941`, `$152,372.00`.
- **Dates:** `DD/MM/YY` or `DD/MM/YYYY`, time as `a las HH:mm` or trailing
  `HH:mm:ss`. Timezone America/Bogotá.
- **Account mask:** `*5005`, `**5005`, or `producto 5005` / `cuenta 5005`
  (the debit/savings account — not a credit card).

| Verb / phrase | Direction | Amount | Counterparty (→ `merchant`) |
|---|---|---|---|
| `Pagaste $X a <NEG>` | EXPENSE | X | the payee `<NEG>` (e.g. `101 FINTECH S A S`, `NU Compania de Financiamiento`) |
| `pagaste $X por codigo QR … a la llave <K>` | EXPENSE | X | the llave `<K>` |
| `$X a la llave @<K> … a <name>` (Bre-b) | EXPENSE | X | `<name>` / the llave |
| `Transferiste $X … a la cuenta *<N>` | EXPENSE | X | account `*<N>` |
| `Recibiste una transferencia por $X de <name>` | INCOME | X | `<name>` |
| `Recibiste una consignacion por $X desde el corresponsal <name>` | INCOME | X | `<name>` |

### 1CERO1 — SMS, sender `891134` (work credit card)

- **Amounts:** Colombian style — `.` groups, `,` decimals: `$3.900,00`,
  `$851.791,00`, `$152.372,00`.
- **Dates:** `YYYY/MM/DD a las HH:mm:ss`.
- **Card mask:** `con la TC 49846720*****5427` → **last4 = 5427**.

| Phrase | Direction | Notes |
|---|---|---|
| `1CERO1 informa realizo Compra <MERCHANT>, … por $X con la TC …5427` | EXPENSE | Real card spend. `<MERCHANT>` e.g. `MERCADO PAGO*MERCADOL`. **Bancolombia never sees this** (different account) — must be captured. |
| `1CERO1 informa realizo Pago, … por $X con la TC …5427` | EXPENSE | Credit-card **bill payment**. Its cash counterpart is a Bancolombia `Pagaste` for the same amount/date — the prime duplicate case (see the `$152.372` pair below). Stage it; let the inbox flag the dup. |
| `1CERO1 informa realizo Cambio Clave, … con la TC …5427` | **DROP** | No amount, not a movement (PIN change). |

### Nu — push notification (scaffold only, not parsed this sprint)

Documented now so the fast-follow rule set is ready.

- **Amounts:** Colombian style: `$180.720,00`, `$13.300,00`.
- **Dates:** relative (`Hoy • 11:40`, `09 jul`) → **fall back to the
  notification's posted timestamp**, since the body carries no absolute date.
- **Card mask:** `tu tarjeta terminada en 3101` → last4 = 3101; or
  `con tu cuenta de ahorros` (no digits → no card match).

| Phrase | Direction | Notes |
|---|---|---|
| `Compra aprobada por $X … en <MERCHANT> … terminada en 3101` | EXPENSE | e.g. `GOOGLE YouTube`. |
| `Pago aprobado por $X - Pagaste en <MERCHANT>` | EXPENSE | e.g. `GLOBAL COLOMBIA 81 SA`. |
| `¡Bravo! Pagaste tu tarjeta de crédito Nu … $X` | EXPENSE | CC bill payment; duplicates a Bancolombia `Pagaste a NU`. |
| `Tienes un pago por $X de <name> … Completa tu pago` | **DROP** | A payment *request* / pre-auth, not a completed movement (note the amount even differs from the approved charge: `$180.000` request vs `$180.720` approved). |

### Noise to drop (never stage)

A capture is staged **only** when the parser matches a known transaction
template that yields *both* an amount *and* a direction. Everything else is
discarded (logged at debug for rule-tuning, never surfaced): PIN changes,
payment requests, marketing, balance nudges, OTP codes. Silent-drop is the
default — a false "detected movement" is worse than a missed one.

---

## Workstream A — SMS capture source + permissions

**Goal:** turn incoming and historical SMS from allow-listed senders into a
normalized `RawCapture`, on-device, privacy-first.

- **Permissions:** `RECEIVE_SMS` (live) + `READ_SMS` (backfill). Add a clear
  onboarding explainer screen: *why* (to detect movements automatically),
  *what stays local* (messages are parsed on the device; raw text never leaves
  it and is never uploaded to Firestore). Gate everything behind an explicit
  opt-in toggle. `READ_SMS` is Play-restricted — fine for the sideloaded APK;
  note in the explainer that this is a personal build.
- **Live receive:** a `BroadcastReceiver` on `SMS_RECEIVED` →
  `RawCapture(sender, body, receivedAt = now, channel = SMS)`.
- **Backfill:** on first grant, read existing SMS via the Telephony
  `content://sms/inbox` `ContentProvider`, filtered to the sender allowlist and
  a bounded window (e.g. last 90 days), so the inbox isn't flooded on day one.
  De-dupe backfill against already-staged/committed items.
- **Sender allowlist (data, extensible):** `85540` → Bancolombia,
  `891134` → 1CERO1. Adding a bank = adding an entry, not editing a `when`.
- **Seam for notifications:** define the `CaptureSource` interface and a
  `NotificationListenerService` subclass **stubbed** behind it with a
  `// TODO(sprint-4):` — SMS is the only live source this sprint, but the
  parser and pending store must not care which source fed them.

```kotlin
// core/data/capture/RawCapture.kt  (pure, no Android types)
enum class CaptureChannel { SMS, NOTIFICATION }

data class RawCapture(
    val sender: String,        // "85540", "891134", or a package name later
    val body: String,
    val receivedAt: Instant,   // used as the date fallback for relative-dated messages
    val channel: CaptureChannel,
)
```

## Workstream B — the parser (rule sets as data)

**Location:** `core/data/capture/parser/`. The parsing logic is **pure Kotlin,
no Android imports**, so it runs under `testDebugUnitTest`. Rule sets are data,
per `ARCHITECTURE.md` — one issuer rule object per bank, matched by sender.

```kotlin
// core/data/repository → new contract in core/domain/repository
sealed interface ParseResult {
    data class Recognized(val pending: PendingTransaction) : ParseResult
    data class Ignored(val reason: String) : ParseResult   // known-noise, dropped
    data object Unrecognized : ParseResult                  // no rule matched → dropped + debug-logged
}

interface TransactionParser {
    fun parse(raw: RawCapture): ParseResult
}
```

Each `IssuerRule` carries: the sender it matches, its **decimal convention**
(Bancolombia = dot-decimal / comma-group; 1CERO1 = comma-decimal / dot-group),
an ordered list of message templates (regex + a builder that pulls the fields),
and its **drop** patterns (PIN change, requests, marketing).

The parser must produce, per recognized message:

1. **Amount → `Money` (cents).** A pure normalizer that takes the issuer's
   separator convention: strip `$`/spaces, remove the group separator, treat the
   decimal separator as `.`, then convert with **integer math** (pesos × 100 +
   2-digit centavos) — never `Double`. Handles the no-decimal forms
   (`$1,487,941`, `$10,000`). For an unknown sender, fall back to the heuristic
   "the separator with exactly two trailing digits is the decimal, all others
   are grouping" (COP display is whole pesos anyway). **Table-test every sample
   amount in the appendix.**
2. **Direction → `TransactionType`.** Keyword-driven per the format tables:
   `Recibiste …` ⇒ INCOME; `Pagaste / Transferiste / Compra / pagaste por QR /
   a la llave … desde` ⇒ EXPENSE.
3. **Date → `Instant`.** Normalize `DD/MM/YY(YY)`, `DD/MM/YYYY HH:mm:ss`,
   `YYYY/MM/DD a las HH:mm:ss` against `America/Bogota`. When the body has no
   absolute date (Nu's relative dates), use `RawCapture.receivedAt`.
4. **Card match → `cardId`.** Extract last4 (`*5005`, `**5005`, `producto
   5005`, `…*****5427`, `terminada en 3101`) and match against the registered
   `Card.last4`. On a hit, set `cardId`; on a miss, leave `cardId = null` and
   keep the raw mask for the review sheet. `bank` is set from the issuer.
   *(Tip for the user: registering the Bancolombia account as a Card
   `last4 = 5005` makes those captures auto-match — no schema change needed.)*
5. **Merchant → `merchant`.** The payee/counterparty text per the tables
   (`101 FINTECH S A S`, `MERCADO PAGO*MERCADOL`, `GOOGLE YouTube`,
   `ANDRES OSORNO`, `MULTIPAGAS UNICENTRO …`).
6. **Category:** **not** guessed by the parser. Captured items are staged
   uncategorized (default `Otros`); the user assigns a category at review. Never
   surface raw parser output as if it were a finished, categorized movement.
7. **Method:** always `PaymentMethod.CARD` (cash never notifies).

**Confidence & method:** captured movements carry `source = SMS` (preserve
provenance — do not rewrite to `MANUAL` on confirm). A field-completeness score
(amount + date + direction + matched card) can order the inbox but is not a
gate; the user is the gate.

## Workstream C — the pending inbox (review UX)

The third planning pillar. Nothing here writes to the ledger until the user
confirms.

**Surfacing.** The bottom bar is full (Inicio · Movimientos · +FAB ·
Recordatorios · Perfil), so the inbox is **not** a new tab:

- A **dashboard banner card** at the top of `DashboardScreen` when the pending
  count > 0: *"Detectamos N movimientos nuevos"* + a `Revisar` action, styled
  on-palette (`VtSurfaceVariant` tile, gold `Revisar`). Per `DESIGN_SYSTEM.md`,
  phrase captures as "Detectamos un movimiento", never as raw text.
- Tapping it opens a dedicated **`PendingReviewScreen`** (new type-safe route
  `PendingRoute` in `navigation/TintoRoutes.kt`). Optionally mirror the count as
  a small badge on the Perfil "Captura automática" row.

**The review list.** One row per pending item, reusing `StatementRow`
vocabulary: proposed amount as `MoneyText` (expense bordeaux / income sage),
proposed merchant, a source chip (`SMS · Bancolombia`), the date, and the
matched card mask or *"Sin tarjeta"*. Empty state when nothing is pending.

**The review sheet** (tap a row): editable, confirm-before-commit —

- Fields: amount, type toggle (Gasto / Ingreso), **category picker (required)**,
  method/card, merchant, date. Pre-filled from the parse.
- Actions: **Confirmar** → promote via
  `TransactionRepository.addTransaction(...)` (Firestore when signed in /
  InMemory in demo, `source = SMS`), then remove from the pending store.
  **Descartar** → delete from the pending store (no ledger write).
- Optional convenience: **Confirmar todos** for the trivial-parse case.

**Duplicate warning.** Before confirm, flag likely duplicates — a pending item
whose `(amount, last4, occurredAt ± a window)` matches an already-committed
`Transaction` **or** another pending item — with a *"Posible duplicado"* badge
and a pre-selected Descartar. This is the guardrail for the "capture as
expenses" decision: the `$357.509` (Nu ↔ Bancolombia) and `$152.372`
(1CERO1 ↔ Bancolombia) pairs must be visibly reconcilable, not silently doubled.
Surface, never auto-merge (`ARCHITECTURE.md` dedup rule).

---

## Staging store — device-local

Captured items are **device-local and pre-user-data** until confirmed: SMS
lives on the phone, arrives while the app is backgrounded or signed out, and a
raw parse is not something to replicate into `users/{uid}/…`. So the staging
store does **not** follow the `Synced*` / `InMemory*` session split.

**Recommendation:** a local **Room** `pending_transactions` table (this is the
sprint that introduces Room + KSP — a clear, documented reason per
`ARCHITECTURE.md` "a Room staging layer may still appear in the capture
sprints"), fronted by a single `PendingTransactionRepository` exposing
`Flow<List<PendingTransaction>>`. On **Confirmar**, the promoted `Transaction`
goes through the existing session-routed `TransactionRepository` — so the
committed ledger stays exactly where it is today. *(Lighter alternative, if we'd
rather not add Room yet: a DataStore-backed JSON list. Flagged as an open
question — Room is preferred for the dedup query and growth.)*

```kotlin
// core/domain/model/PendingTransaction.kt
data class PendingTransaction(
    val id: String,                 // UUID
    val channel: CaptureChannel,
    val issuer: String,             // "Bancolombia" / "1CERO1"
    val rawBody: String,            // kept for the review sheet + rule-tuning; device-local only
    val type: TransactionType,
    val amount: Money,
    val last4: String?,             // parsed mask, may not match a registered card
    val cardId: String?,            // set when last4 matched a Card
    val bank: String?,
    val merchant: String?,
    val occurredAt: Instant,
    val capturedAt: Instant,
)
```

## Cross-cutting rules

- **Never auto-commit.** Parsed items always pass through
  `pending_transactions` and explicit user confirmation.
- **Rule sets are data.** Adding a bank/sender is adding a rule object + an
  allowlist entry — never a new `when` branch in the parser core.
- **Layer purity.** Regex/amount/date normalizers are pure Kotlin (JVM-testable,
  no Android). The `BroadcastReceiver`/`ContentProvider`/`NotificationListener`
  Android glue stays in `core/data/capture/*` and never leaks above the data
  layer. No persistence types in `feature/*`.
- **Money is `Money(cents: Long)`** everywhere; formatting only through
  `MoneyFormat` / `MoneyText`. Cards keep **4** digits.
- **No Firestore schema change.** Committed captures reuse the existing
  `Transaction` document and field names (`source` already supports `SMS`).
- **Strings** in `res/values/strings.xml`, Spanish, following the existing
  naming (`capture_*`, `pending_*`, `pending_duplicate_*`, `action_confirm`,
  `action_discard`).

## Analytics & privacy

Behind `core/common/TintoAnalytics`, **no amounts, merchants, senders, or raw
text** — ever. Events: `capture_permission_granted`,
`capture_detected` (params: channel + issuer only), `pending_confirmed`,
`pending_discarded`, `pending_duplicate_shown`. Raw SMS bodies stay in the local
Room store and are never written to Firestore or an analytics payload.

## Out of scope (leave `// TODO(sprint-4):` seams)

`NotificationListenerService` implementation (interface + stub only), Gmail
parsing, auto-categorization, recurring-charge detection, transfer/own-account
auto-classification or auto-merge, JSON export changes, iOS.

## Open questions to settle during build

1. **Room vs DataStore for staging** — Room recommended; confirm before adding
   the dependency.
2. **Backfill window** — 90 days? all inbox? (Bigger = noisier first run.)
3. **Bancolombia account `5005`** — register it as a `Card(last4 = 5005)` for
   auto-match, or introduce a lightweight "account" source concept later?
   (Recommend the former this sprint — zero schema change.)
4. **Duplicate window** — how wide is "same time"? Start at ±10 min on identical
   amount + last4, tune against real pairs.
5. **1CERO1 `Pago` vs Bancolombia `Pagaste`** — both stage as expenses by
   decision; confirm the duplicate warning is enough (no special-casing).

## Definition of done

- `./gradlew assembleDebug testDebugUnitTest lint` green on CI (push and let CI
  verify — the container can't reach Google's Maven; iterate until green).
- **Table-driven parser tests** over the verbatim sample messages in the
  appendix: amount (both separator conventions + no-decimal), date (all three
  layouts), direction, and last4 extraction for Bancolombia and 1CERO1;
  plus the drop cases (`Cambio Clave`, `Tienes un pago`).
- Live SMS receive **and** bounded backfill both stage into
  `pending_transactions`; confirm promotes to the ledger with `source = SMS`
  preserved; discard removes without a ledger write.
- The duplicate warning fires for the `$152.372` and `$357.509` pairs.
- Works in demo mode (stages locally, confirm writes to `InMemory`) and
  signed-in mode (confirm writes to Firestore).
- Permission explainer + opt-in toggle present; nothing is read before opt-in.
- Docs touched: `README.md` status line, `CLAUDE.md` sprint section, and
  `ARCHITECTURE.md` if the Room-staging decision is confirmed.

---

## Appendix — real sample messages (test fixtures, verbatim)

**Bancolombia (`85540`)**

```
$15,000.00 a la llave @samueld7650 desde tu cuenta *5005 a samuel diaz el 07/07/26 a las 14:45. Con Bre-b es de una y gratis. Dudas al 018000912345.
Bancolombia: BRAYAN ROMERO DORADO pagaste $18,000.00 por codigo QR desde tu cuenta *5005 a la llave 0089378571 el 09/07/2026 a las 08:50. Con codigo QR es facil y de una. Dudas al 018000912345.
Bancolombia: Recibiste una transferencia por $10,000 de ANDRES OSORNO en tu cuenta **5005, el 14/07/2026 a las 12:15. Si tienes dudas, hablemos: 018000931987. Siempre a tu lado.
Bancolombia: Recibiste una consignacion por $1,487,941 desde el corresponsal MULTIPAGAS UNICENTRO CENTRO CO en MEDELLIN, el 15/07/26 16:56. Si tienes dudas, llamanos: 018000931987. A tu lado siempre.
Bancolombia: Pagaste $152,372.00 a 101 FINTECH S A S desde tu producto 5005 el 16/07/2026 10:07:26. ¿Dudas? Llamanos al 6045109095. Estamos cerca
Bancolombia: Pagaste $357,509.00 a NU Compania de Financiamiento desde tu producto 5005 el 16/07/2026 10:34:42. ¿Dudas? Llamanos al 6045109095. Estamos cerca
Bancolombia: Transferiste $150,000.00 desde tu cuenta 5005 a la cuenta *3105687364 el 16/07/2026 a las 16:21. ¿Dudas? Llamanos al 018000931987. Estamos cerca.
```

**1CERO1 (`891134`)**

```
1CERO1 informa realizo Compra ..., el 2026/05/20 a las 09:11:57 por $3.900,00 con la TC 49846720*****5427 .
1CERO1 informa realizo Cambio Clave, el 2026/05/25 a las 10:32:51 con la TC 49846720*****5427 .        # DROP
1CERO1 informa realizo Compra MERCADO PAGO*MERCADOL, el 2026/06/25 a las 11:16:59 por $851.791,00 con la TC 49846720*****5427 .
1CERO1 informa realizo Pago, el 2026/07/16 a las 10:08:08 por $152.372,00 con la TC 49846720*****5427 .   # dup of Bancolombia "Pagaste $152,372.00"
```

**Nu (push — fast-follow rule set, documented not parsed this sprint)**

```
Pago aprobado por $180.720,00 - Pagaste en GLOBAL COLOMBIA 81 SA con tu cuenta de ahorros. Si tienes dudas contáctanos via chat.
Tienes un pago por $180.000,00 de GLOBAL COLOMBIA 81 SA. Completa tu pago de forma fácil y segura en tu app Nu.   # DROP (request)
¡Bravo! Pagaste tu tarjeta de crédito Nu. Recibimos tu pago por $357.509,00. En un rato podrás verlo en tu app.   # dup of Bancolombia "Pagaste $357,509.00 a NU"
Compra aprobada por $13.300,00 - Tu compra en GOOGLE YouTube por $13.300,00 con tu tarjeta terminada en 3101 ha sido APROBADA.
```
