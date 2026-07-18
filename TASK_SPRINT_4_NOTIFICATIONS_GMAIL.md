# TASK_SPRINT_4_NOTIFICATIONS_GMAIL.md — Sprint 4: notification & email capture (Nu + Gmail)

## Where the app stands (context for this sprint)

Sprints 1 (UI shell), 1.5 (Firebase), 2 (real-account CRUD) and 3 (automatic
capture, SMS first) are done and live on `main`, plus the post-Sprint-3 reminder
notifications:

- The **capture pipeline exists and is channel-agnostic.** A `RawCapture`
  (`sender`, `body`, `receivedAt`, `channel`) flows through a pure-Kotlin
  `TransactionParser` (rule sets are data in `IssuerRules`) into a device-local
  Room `pending_transactions` store, and the user reviews everything in
  `PendingReviewScreen` (batch select, per-row category, cross-issuer duplicate
  detection) before a single explicit **Confirmar** promotes it through the
  session-routed `TransactionRepository`. **Nothing auto-commits.**
- **SMS is the only live source** so far: Bancolombia (`85540`) and 1CERO1
  (`891134`), live receive + bounded 90-day backfill, behind an opt-in in Perfil.
- The domain already models every provenance we need:
  `TransactionSource { MANUAL, NOTIFICATION, EMAIL, SMS }` and
  `CaptureChannel { SMS, NOTIFICATION }`.
- **Reminders are already functional** — the "Gmail + reminders" roadmap line for
  this sprint is half-delivered: `TASK_REMINDER_NOTIFICATIONS.md` shipped local
  notifications (channel, in-context permission, alarm scheduling, boot
  re-registration). **Reminders are out of scope here** — this sprint is the two
  remaining automatic-capture channels.

The gap: two `// TODO(sprint-4):` seams left deliberately open in Sprint 3.

1. **Nu push notifications.** `core/data/capture/notification/TintoNotificationListenerService`
   is a registered-nowhere stub; `IssuerRules` has a `TODO(sprint-4)` where the
   Nu rule set goes. Nu is the user's third real money source and, unlike
   Bancolombia/1CERO1, arrives as a **push notification**, not SMS.
2. **Gmail.** Transactional-email parsing — the third capture channel promised in
   `PROJECT_CONTEXT.md` — has no code at all yet.

Both feed the **existing** parser and pending inbox. This sprint is mostly
*sources*, not new UX: wire two `CaptureSource`s into a pipeline that already
knows how to stage, dedupe, and review.

> Read `ARCHITECTURE.md` §"Data-capture pipeline", `TASK_SPRINT_3_CAPTURE.md`
> (the pipeline it describes is the substrate here — reuse it, do not rebuild it),
> and `DESIGN_SYSTEM.md` §"Voice / microcopy" alongside this brief.

## Decisions taken (planning)

| Decision | Choice | Consequence |
|---|---|---|
| Channel A | **Nu push notifications** via `NotificationListenerService` | The `CaptureChannel.NOTIFICATION` seam goes live. Formats are already documented in the Sprint 3 appendix (`§Nu`). |
| Channel B | **Gmail** transactional email, read-only | A new `CaptureChannel.EMAIL` + `TransactionSource.EMAIL`. Auth is an incremental Google OAuth scope on top of the existing Google sign-in. |
| Parsing location | **Same pure-Kotlin parser** | Nu and each email issuer are new `IssuerRule`s. Nu's relative dates fall back to `RawCapture.receivedAt`; email uses the message's `Date` header. Zero parser-core changes. |
| Staging & review | **Reuse `pending_transactions` + `PendingReviewScreen` unchanged** | New sources light up the same dashboard banner, the same duplicate warning, the same confirm/discard. The inbox must not care which of the four channels fed it. |
| Dedup across channels | **Reuse `DetectDuplicates` (amount + last4 + ±window)** | Now four channels can see the same charge (a Nu push *and* the Nu-line Gmail *and* a Bancolombia SMS for the same purchase). The existing warning is the reconciliation point — still surface, never auto-merge. |
| Privacy posture | **On-device parsing; raw bodies stay local** | Notification text and email bodies are parsed on the device into `pending_transactions` (local Room) and are **never** written to Firestore or an analytics payload — identical to the SMS rule. |

Everything a parse produces still lands in the **pending inbox** for user review —
a bad parse must never silently reach the ledger (`CLAUDE.md` guardrail).

## Scope at a glance

**In:** Nu notification capture (live `NotificationListenerService` behind the
existing seam + its `IssuerRule` + drop patterns) · Gmail capture (incremental
read-only OAuth, a bounded fetch of transactional emails from an allow-listed
sender set, email-body parsing into the same pipeline) · the two
permission/onboarding flows (notification-access system settings + Gmail scope
consent) · Perfil toggles for each source · table-driven parser tests over real
Nu and email fixtures.

**Out (do not pull forward):** reminders (already done), auto-categorization / ML,
recurring-subscription detection, transfer/own-account auto-merge, any Firestore
schema change, JSON-export changes, iOS, a background Gmail sync worker beyond a
simple foreground/opt-in fetch (a `WorkManager` cadence can be a *fast-follow* —
scaffold the seam, `// TODO(sprint-5):`).

---

## Channel A — Nu push notifications

The formats are **already the source of truth** in `TASK_SPRINT_3_CAPTURE.md`
§"Nu — push notification" and its appendix. Do not re-derive them; implement them.

### Recap of the Nu rules (from Sprint 3)

- **Sender:** the Nu app package name (e.g. `com.nu.production` — confirm on the
  device) becomes `RawCapture.sender`. The allowlist is by package, extensible
  exactly like the SMS shortcode allowlist.
- **Amounts:** Colombian style (`.` groups, `,` decimals): `$180.720,00`,
  `$13.300,00` → `AmountConvention.COMMA_DECIMAL`.
- **Dates:** relative (`Hoy • 11:40`, `09 jul`) carry no absolute date →
  **fall back to `RawCapture.receivedAt`** (the notification `postTime`).
- **Card mask:** `terminada en 3101` → last4 = 3101; `con tu cuenta de ahorros`
  → no digits → `cardId = null`, keep the raw text for the review sheet.

| Phrase | Direction | Notes |
|---|---|---|
| `Compra aprobada por $X … terminada en 3101` | EXPENSE | e.g. `GOOGLE YouTube`. |
| `Pago aprobado por $X - Pagaste en <MERCHANT>` | EXPENSE | e.g. `GLOBAL COLOMBIA 81 SA`. |
| `¡Bravo! Pagaste tu tarjeta de crédito Nu … $X` | EXPENSE | CC bill payment; duplicates a Bancolombia `Pagaste a NU`. |
| `Tienes un pago por $X de <name> … Completa tu pago` | **DROP** | Payment *request* / pre-auth, not a completed movement. |

### Workstream A1 — the notification source

**Goal:** turn Nu's posted notifications into `RawCapture(channel = NOTIFICATION)`,
feeding the existing `CaptureProcessor`.

- **Implement the existing seam:** register `TintoNotificationListenerService` in
  the manifest (`BIND_NOTIFICATION_LISTENER_SERVICE` + the
  `android.service.notification.NotificationListenerService` intent-filter),
  gated so it does nothing until the user opts in.
- In `onNotificationPosted`, map to
  `RawCapture(sender = sbn.packageName, body = title + " " + text extracted from
  sbn.notification.extras, receivedAt = Instant(sbn.postTime), channel = NOTIFICATION)`
  and feed the same processor the SMS receiver uses. Filter to the Nu package
  allowlist **before** parsing; ignore everything else silently.
- **De-dupe the notification storm.** A single Nu event can post/​update the same
  notification several times. Key by `(packageName, a stable content hash, a
  short time bucket)` so one purchase stages one pending row — mirror the SMS
  backfill's dedup-key discipline so re-posts never resurrect a
  confirmed/discarded item.
- **No history backfill.** `NotificationListenerService` only sees notifications
  posted while it's bound — there is no equivalent of the SMS `content://`
  backfill. Say so in the onboarding copy ("desde ahora", not "los últimos 90
  días").

### Workstream A2 — the Nu rule set

- Add `IssuerRules.nu` (issuer `"Nu"`, bank `"NU Bank"`, senders = the Nu
  package, `AmountConvention.COMMA_DECIMAL`) with the three EXPENSE templates and
  the `Tienes un pago` **drop pattern** above, and add it to `IssuerRules.all`.
  This is a data change — the parser core does not move.
- The relative-date handling already exists (the parser falls back to
  `receivedAt` when a template yields no absolute date) — confirm Nu templates
  simply omit the `date`/`time` groups.

### Workstream A3 — permission & onboarding (notification access)

- Notification access is a **special access** the user grants in system settings
  (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`), not a runtime permission
  dialog. Build the explainer: *why* (detect Nu movements automatically), *what
  stays local* (notifications are read and parsed on the device; text never
  leaves it, never uploaded to Firestore), then deep-link to the settings screen.
- A Perfil "Captura automática" toggle row for **Notificaciones (Nu)**, mirroring
  the SMS row: reflects whether access is granted, routes to the explainer,
  degrades gracefully when the user revokes access in system settings.

---

## Channel B — Gmail transactional email

Gmail is the one genuinely new subsystem this sprint: an OAuth scope, a fetch, and
email-body parsing. Everything downstream is the existing pipeline.

### Workstream B1 — auth (incremental Google OAuth)

- The app already signs in with Google via Credential Manager. Gmail read adds the
  **`https://www.googleapis.com/auth/gmail.readonly`** scope via **incremental
  authorization** — request it *only* when the user opts into email capture, not
  at sign-in. Declining leaves the rest of the app untouched.
- **Demo mode has no Google account** → the email source is simply unavailable in
  demo; the Perfil row explains "Inicia sesión para conectar Gmail". Do not fake
  it with mock emails in the ledger (mock *fixtures* live only in tests).
- Keep all Google-API/auth glue in `core/data/capture/email/*`; no Google API
  types above the data layer. Token handling uses the platform account/authorizer
  — do not persist raw tokens ourselves.
- Note `FIREBASE_SETUP.md`: the Gmail scope may need to be added to the OAuth
  consent screen / verified-scopes list in the Google Cloud console. Document the
  console step there; for the personal sideloaded build, "testing" users suffice.

### Workstream B2 — the email source

**Goal:** turn transactional emails into `RawCapture(channel = EMAIL)`.

- **Sender allowlist (data, extensible):** the bank/fintech `From:` addresses
  (e.g. Bancolombia, Nu, 1CERO1 email domains — collect the real ones as
  fixtures, see B4). Adding an issuer = adding an allowlist entry + a rule, never
  a `when` branch.
- **Bounded fetch, not a live push.** On opt-in (and on a manual "Buscar ahora"),
  query the Gmail API (`users.messages.list` with a `from:(…) newer_than:90d`
  query built from the allowlist) and fetch each message. Bound the window
  exactly like the SMS backfill so the first run isn't a flood. A periodic
  `WorkManager` refresh is a **`// TODO(sprint-5):`** seam — this sprint ships the
  on-demand fetch.
- **Map to `RawCapture`:** `sender = the From address`, `body = the plain-text
  part (or HTML stripped to text)`, `receivedAt = the message Date header`,
  `channel = EMAIL`. HTML→text extraction stays in the data layer; the parser sees
  clean text like every other channel.
- **De-dupe by Gmail message id** so re-fetching the same window never re-stages a
  message the user already confirmed or discarded (same dedup-key discipline as
  SMS/notifications).

### Workstream B3 — email rule sets

- Add one `IssuerRule` per email issuer under `IssuerRules`, keyed by the `From`
  address, reusing the amount/date/last4 machinery. Email bodies are wordier than
  SMS — templates match the transactional sentence, drop patterns cover
  statements, marketing, and OTP/security emails.
- **Dates come from the email `Date` header** (absolute) — prefer it over any date
  printed in the body unless a body date is clearly the transaction date; keep the
  header as the reliable fallback.

### Workstream B4 — fixtures (blocking prerequisite)

Unlike Nu (already documented), the **real email formats are not yet captured.**
Before writing B3 rules, collect verbatim sample emails from the user's actual
bank senders (a handful per issuer: a purchase, an income/transfer, and at least
one noise email to drop) and paste them into this file's appendix as test
fixtures — exactly as the Sprint 3 appendix did for SMS. The parser tests are
table-driven over these verbatim strings. **No fixtures → no email rules;** ship
Nu alone and carry Gmail if the samples aren't available.

> Tooling note: a Gmail MCP is connected to this session. It may be used to help
> *collect fixture text* with the user's consent, but the shipped app reads Gmail
> only through the on-device Gmail API path above — the MCP is not a runtime
> dependency of the app.

---

## Review UX — reuse, with two small touches

The pending inbox does not change structurally. Two small, on-palette additions:

- **Source chips** already read `SMS · Bancolombia`; they now also render
  `Notificación · Nu` and `Correo · Nu` (or the issuer). One shared mapping,
  strings in `strings.xml` (`capture_channel_notification`, `capture_channel_email`).
- The **dashboard banner** copy is channel-agnostic already ("Detectamos N
  movimientos") — verify it still reads correctly when the N spans all four
  channels. No new screen, no new route.

Duplicate detection is unchanged code but newly important: the Nu purchase push,
the Nu email, and the Bancolombia settlement SMS can all describe one payment.
Confirm `DetectDuplicates` groups them (identical amount + last4 within the
window) and starts the extras **unselected** with the *"Posible duplicado"* badge.

---

## Cross-cutting rules

- **Never auto-commit.** Notification- and email-parsed items pass through
  `pending_transactions` and explicit user confirmation, same as SMS.
- **Rule sets are data.** Nu and every email issuer are `IssuerRule` objects +
  allowlist entries — no new `when` branch in the parser core.
- **Preserve provenance.** Confirmed captures carry `source = NOTIFICATION` or
  `source = EMAIL` (do not rewrite to `MANUAL`), `channel` set accordingly.
- **Layer purity.** The `NotificationListenerService`, the Gmail API client, and
  HTML→text extraction are Android/data-layer glue in `core/data/capture/*` and
  never leak above the data layer. Amount/date/last4 normalizers stay pure Kotlin
  (JVM-testable). No persistence or Google/Android types in `feature/*`.
- **Money is `Money(cents: Long)`** everywhere; formatting only through
  `MoneyFormat` / `MoneyText`. Cards keep **4** digits.
- **No Firestore schema change.** Committed captures reuse the existing
  `Transaction` document; `source` already supports `NOTIFICATION` and `EMAIL`.
- **Strings** in `res/values/strings.xml`, Spanish, following existing naming
  (`capture_*`, `pending_*`, `action_confirm`, `action_discard`, new
  `capture_notification_*` / `capture_email_*`).

## Analytics & privacy

Behind `core/common/TintoAnalytics`, **no amounts, merchants, senders, subjects,
or raw body/email text** — ever. Extend the existing events with the new channels
only: `capture_permission_granted` (param: channel), `capture_detected` (params:
channel + issuer only), plus `gmail_scope_granted` (no address). Raw notification
text and email bodies stay in the local Room store and are never written to
Firestore or an analytics payload — identical to the SMS rule.

## Out of scope (leave `// TODO(sprint-5):` seams)

Reminders (done), a background/periodic Gmail sync worker (on-demand fetch only
this sprint), auto-categorization/ML, recurring-charge detection,
transfer/own-account auto-classification or auto-merge, cross-channel auto-merge,
JSON-export changes, iOS.

## Open questions to settle during build

1. **Nu package name** — confirm the exact `packageName` (`com.nu.production`?) on
   the real device before pinning the allowlist entry.
2. **Notification extras extraction** — `EXTRA_TITLE` + `EXTRA_TEXT` vs
   `EXTRA_BIG_TEXT`; some Nu notifications carry the amount only in the expanded
   text. Verify against real posts.
3. **Gmail scope approval** — does the personal build's OAuth consent need the
   sensitive `gmail.readonly` scope added as a test user, or is verification
   required? (Testing users should suffice for a sideloaded personal app.)
4. **Email fixtures availability** — B4 is a hard prerequisite for B3. If the user
   can't supply real sample emails this sprint, ship Nu notifications alone and
   carry Gmail to a follow-up.
5. **HTML email parsing** — a lightweight AndroidX/first-party HTML→text approach
   (e.g. `Html.fromHtml` + cleanup) vs adding a parser dependency. Prefer no new
   dependency; justify in the commit if one is genuinely warranted
   (`CLAUDE.md` guardrail).
6. **Fetch window** — 90 days to match SMS backfill? Bigger = noisier first run.

## Definition of done

- `./gradlew assembleDebug testDebugUnitTest lint` green on CI (push and let CI
  verify — the container can't reach Google's Maven; iterate until green).
- **Nu:** the `NotificationListenerService` is registered and, once access is
  granted, live Nu notifications stage into `pending_transactions`; the Nu
  `IssuerRule` is table-tested over the appendix fixtures (all three EXPENSE
  templates + the `Tienes un pago` drop + `terminada en 3101` last4 +
  relative-date → `receivedAt` fallback).
- **Gmail:** opt-in triggers the incremental `gmail.readonly` consent; a bounded
  fetch of allow-listed senders stages emails into `pending_transactions`; email
  `IssuerRule`s are table-tested over real fixtures in the appendix
  (or Gmail is explicitly deferred per open question 4, and the brief/README say
  so).
- Confirm promotes to the ledger with `source = NOTIFICATION` / `EMAIL` and the
  correct `channel` preserved; discard removes without a ledger write.
- The duplicate warning groups a same-amount, same-last4 charge seen across SMS,
  notification, and email within the window.
- Works in demo mode (notifications stage locally, confirm writes to `InMemory`;
  Gmail correctly unavailable) and signed-in mode (Gmail available, confirm writes
  to Firestore).
- Both permission flows present (notification-access deep link + Gmail consent),
  each behind its own Perfil opt-in; nothing is read before opt-in.
- Docs touched: `README.md` status line (add "Sprint 4: Nu + Gmail capture ✅"),
  `CLAUDE.md` sprint section, `ARCHITECTURE.md` if the Gmail-auth decision is
  worth pinning, and `FIREBASE_SETUP.md` for the Gmail OAuth-scope console step.
  Remove the `// TODO(sprint-4):` markers as their seams go live.

---

## Appendix — real sample messages (test fixtures, verbatim)

### Nu (push notification — package `com.nu.production`, confirm)

Documented in Sprint 3; parsed for real this sprint.

```
Pago aprobado por $180.720,00 - Pagaste en GLOBAL COLOMBIA 81 SA con tu cuenta de ahorros. Si tienes dudas contáctanos via chat.
Tienes un pago por $180.000,00 de GLOBAL COLOMBIA 81 SA. Completa tu pago de forma fácil y segura en tu app Nu.   # DROP (request)
¡Bravo! Pagaste tu tarjeta de crédito Nu. Recibimos tu pago por $357.509,00. En un rato podrás verlo en tu app.   # dup of Bancolombia "Pagaste $357,509.00 a NU"
Compra aprobada por $13.300,00 - Tu compra en GOOGLE YouTube por $13.300,00 con tu tarjeta terminada en 3101 ha sido APROBADA.
```

### Gmail (transactional email — TO BE COLLECTED, see Workstream B4)

> Paste verbatim sample emails here before writing the email rule sets: for each
> bank/fintech email sender, include the `From:` address, the `Subject:`, the
> `Date:` header, and the body text — a purchase, an income/transfer, and at least
> one noise email to drop. These become the table-driven parser fixtures.

```
From:
Subject:
Date:

<body>
```
