# CLAUDE.md — Tinto

Operating manual for Claude Code on this repository. Read `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, and `DESIGN_SYSTEM.md` before writing code — they are the source of truth for product scope, structure, and visual identity. This file is the how-we-work layer on top.

## Project

- **Tinto** — a native Android personal-finance app. Dark, wine-toned ("Vino Tinto"). Offline-first with a Firebase backend.
- Package: `dev.romerobrayan.tinto` (rename the suffix if the app name changes).
- Stack: Kotlin, Jetpack Compose, Material 3, Hilt, Navigation Compose, coroutines/Flow, Firebase (Auth + Google sign-in via Credential Manager, Cloud Firestore, Analytics, Crashlytics).
- Architecture: Clean Architecture + MVVM, package-by-feature. Layer rules are in `ARCHITECTURE.md`.
- `compileSdk 35`, `targetSdk 35`, `minSdk 26`.

## Firebase state (Sprint 1.5 — done)

- Session gate in `TintoRoot`: `Loading → SignedOut (LoginScreen) → Demo | SignedIn (TintoApp)`. Auth contract is `core/domain/repository/AuthRepository`; impl `core/data/auth/FirebaseAuthRepository`.
- Data: `Synced*Repository` classes route by session — Cloud Firestore under `users/{uid}/…` when signed in (offline cache on by default), the `InMemory*` sample data in demo mode. Manual mappers in `core/data/firebase/FirestoreMappers.kt` (`Money` as cents `Long`, `Instant` as epoch millis, `LocalDate` as ISO string) — field names are a persisted schema; don't rename them casually.
- Analytics behind `core/common/TintoAnalytics` (no Firebase types in features); events carry **no amounts/merchants/PII**.
- `app/google-services.json` in the repo may be the placeholder; real console setup steps live in `FIREBASE_SETUP.md`. `app/debug.keystore` is committed on purpose (shared debug signature for local + CI so the registered SHA-1 stays valid).
- Firestore security rules: `firestore.rules` (per-user isolation).

## Current sprint — Sprint 5: 2 categorías nuevas + automatización (done)

Brief in the task prompt. Delivered in four phases (A→D):

- **Categorías con scope.** New `CategoryScope { EXPENSE, INCOME }` + `Category.scope` (default EXPENSE; `TransactionType.toCategoryScope()`). Two new expense categories — **Hogar** (`cat-hogar`, iconKey `home`) and **Emergencias** (`cat-emergencias`, `alert`) — plus a dedicated income set: `cat-nomina`/`cat-pago-deuda`/`cat-prestamo`/`cat-movimiento`/`cat-aportes` (iconKeys `payroll`/`debt`/`loan`/`transfer`/`contribution`, all mapped in `CategoryIcon.glyphFor`, on-brand accents). `FirestoreMappers` persists `scope` (fallback EXPENSE for old rows). Because seeding only runs on an empty remote collection, `SyncedCategoryRepository` gained an **idempotent backfill** that upserts missing system categories by fixed id into existing accounts. The add/edit form filters categories by the selected type's scope and resets an off-scope pick on type change.
- **Formulario de ingreso.** `PaymentMethod` gained **`TRANSFER`** (additive, income-only). In income mode the method is Efectivo / Transferencia / a registered card (chosen by tap, **no last4 prompt**); expense mode is unchanged. `AddTransactionValidator` now takes the `type` and only requires last4 for card **expenses**. `MovementUi.isTransfer` → "Transferencia" in `StatementRow`/detail.
- **Automatización.** `TransactionFrequency { DAILY, WEEKLY, SEMIMONTHLY, MONTHLY }` (do **not** reuse reminder `Recurrence`). Pure `RecurringRuleRollover` (`advanced()`/`plusFrequency()`/`nextHalfMonth()`) — **quincenal = the 15th and the last day** of the month on fixed calendar dates. `GenerateDueOccurrences` is a pure catch-up generator with a deterministic id (`auto-{ruleId}-{yyyyMMdd}`) → idempotent. `TransactionSource.RECURRING` (tolerant mapper) shows the `RecurringBadge`. `RecurringRuleRepository` (Synced/InMemory, `users/{uid}/recurring_rules`), `RecurringTransactionCoordinator` (started from `TintoApplication`, observes the repo, materializes due movements **directly into the ledger** — the anti-autocommit rule guards *parses*, not user-created rules) + `RecurringBootReceiver`. Form: a "Automatizar este movimiento" switch → frequency selector; saving with it on writes occurrence #1 **and** a `RecurringRule`. A lightweight "Movimientos automáticos" screen in Perfil lists rules with pause/resume + delete.
- **Ingresos por captura.** `PendingReviewViewModel` defaults income captures to `cat-movimiento` (expenses stay `cat-otros`); confirming an income capture shows the income fields + automate switch. **Nu income templates deferred** — `// TODO(sprint-6)` left in `IssuerRules.nu`; income arrives from the manual form and Bancolombia SMS only.

Analytics: `recurring_rule_created` carries the frequency name only. Tests: `RecurringRuleRolloverTest`, `GenerateDueOccurrencesTest`, extended `AddTransactionValidatorTest`.

## Previous sprint — Sprint 4 phase 1: Nu notification capture (done; Gmail pending)

Brief: `TASK_SPRINT_4_NOTIFICATIONS_GMAIL.md`. Phase 1 (Channel A) delivered: `TintoNotificationListenerService` implemented and registered (`BIND_NOTIFICATION_LISTENER_SERVICE`), gated on a new opt-in in Perfil ("Notificaciones (Nu)") **and** the system notification-access grant — the explainer deep-links to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`, the row re-checks access on resume and degrades to "Sin acceso" if revoked. Posted notifications from the Nu package (allowlist = `IssuerRules.NU_PACKAGE_NAME` = `com.nu.production`, confirmed on the real device) map to `RawCapture(channel = NOTIFICATION)` (title + EXTRA_BIG_TEXT/EXTRA_TEXT, `receivedAt = postTime`) and feed the same `CaptureProcessor`/pending inbox as SMS. `IssuerRules.nu` (issuer "Nu", bank "NU Bank", `COMMA_DECIMAL`): three EXPENSE templates + the `Tienes un pago` drop; relative dates fall back to `receivedAt` (templates carry no date groups). Parser sender lookup is verbatim-then-digits (package names have no digits). Notification re-emissions collapse via a time-bucketed dedup key (`PendingTransactionEntity.dedupKeyOf`, 10-min bucket for NOTIFICATION only — SMS keys unchanged). **No history backfill** ("desde ahora"). `capture_permission_granted` now carries a `channel` param — still no amounts/merchants/raw text. Confirm promotes with `source = NOTIFICATION`. **Phase 2 (Gmail, Channel B) is not started** — no OAuth scope, no email code.

## Previous sprint — Sprint 3: automatic capture, SMS first (done)

Brief: `TASK_SPRINT_3_CAPTURE.md`. Delivered: SMS capture behind an explicit opt-in in Perfil (`RECEIVE_SMS` live receive via `core/data/capture/SmsCaptureReceiver` + bounded 90-day `READ_SMS` backfill in `SmsBackfill`); a pure-Kotlin rule-set parser (`core/data/capture/parser/` — rules are data in `IssuerRules`: Bancolombia `85540` US-style amounts, 1CERO1 `891134` Colombian-style, three date layouts vs America/Bogota, last4 masks, drop patterns) staged into a Room `pending_transactions` store (`core/data/local/`, the sprint that introduced Room + KSP; confirmed/discarded rows keep their dedup keys so re-backfills never resurrect them). Review UX: dashboard banner ("Detectamos N movimientos") → `PendingReviewScreen` (`feature/pending/`) with **batch multi-select + select-all**, a per-row category chip (default `Otros`, picker sheet), and cross-issuer duplicate detection (`core/domain/usecase/DetectDuplicates.kt`, amount + ±10 min window) — duplicates get a "Posible duplicado" badge and start **unselected**; the user selects and either Agregar or Descartar. Row tap opens the add-transaction form in confirm mode (`AddTransactionRoute(pendingId)`) with an explicit Descartar. Confirm promotes through the session-routed `TransactionRepository` with `source = SMS` preserved. **Never auto-commit a parse.** `NotificationListenerService` (Nu) went live in Sprint 4 phase 1; Gmail remains a pending seam.

## Reminder notifications (done, after Sprint 3)

Brief: `TASK_REMINDER_NOTIFICATIONS.md`. Delivered: `reminders` notification channel; in-context `POST_NOTIFICATIONS` ask on first reminder save (Android 13+, declining degrades gracefully); trigger instant as a pure function (`core/domain/usecase/ReminderTrigger.kt`, `dueTime` or the 08:00 default, unit-tested); inexact `AlarmManager.setAndAllowWhileIdle` alarms keyed by reminder-id hash (`core/data/notifications/ReminderAlarmScheduler`); a repository-observing `ReminderNotificationCoordinator` started from `TintoApplication` (demo/signed-in identical, signed-out cancels all, covers the recurrence rollover); `BOOT_COMPLETED`/`TIMEZONE_CHANGED`/`TIME_SET` re-registration; notification tap lands on Recordatorios. Analytics carries recurrence only — never titles/amounts.

## Previous sprint — Sprint 2: real-account CRUD (done)

Brief: `TASK_SPRINT_2_CRUD.md`. Delivered: movement detail bottom sheet (`core/designsystem/component/MovementDetailSheet.kt`) with edit (add-transaction screen in edit mode via `AddTransactionRoute(transactionId)`) and delete; card CRUD in Perfil (`feature/profile/CardFormSheet.kt`); reminders create/edit/delete/mark-paid (`feature/reminders/ReminderFormSheet.kt`) with the recurrence rollover as a pure function in `core/domain/usecase/ReminderRollover.kt` (unit-tested, month-end clamping pinned). All CRUD routes by session through the `Synced*`/`InMemory*` repositories — identical behavior demo vs signed-in; Firestore writes stay fire-and-forget. Shared form primitives extracted to `core/designsystem/component` (`TintoSelectorPill`, `TintoDatePickerDialog`, `TintoTimePickerDialog`, `TintoConfirmDialog`, `tintoTextFieldColors`). Post-sprint refinements: the card form's bank is a fixed dropdown (Bancolombia/NU Bank/Global66/Daviplata/Nequi/101Fintech), reminders carry an optional `dueTime` (`LocalTime`, Firestore field `dueTime` as "HH:MM" — additive, no renames), and the dashboard chart toggles Gastos/Ingresos (`AggregateSpendUseCase` takes a `TransactionType`).

## Earlier sprint — Sprint 1: UI shell

**Build the complete visual shell wired to in-memory mock data. No Room, no capture, no permissions this sprint.** Deliverable: a navigable, on-brand app that can be clicked through end to end.

Scope for this sprint:

1. **Design system first.** Implement `core/designsystem` fully: `Color.kt`, `Type.kt` (bundle Fraunces + Inter under `res/font/`), `Shape.kt`, `TintoTheme.kt`, `LocalTintoColors`, and the core components listed in `DESIGN_SYSTEM.md` (`TintoBarChart`, `PeriodSelector`, `MonthSelector`, `StatementRow`, `MoneyText`, `CategoryIcon`, `RecurringBadge`, `TintoScaffold`/`TintoBottomBar`).
2. **Navigation.** Single `MainActivity` hosting a `NavHost` with the 5 destinations + the center FAB routing to the add-transaction screen.
3. **Screens (mock-backed):**
   - `DashboardScreen` — month selector, `PeriodSelector`, `TintoBarChart`, money hero + MoM comparison chip, and a short "Movimientos" preview list.
   - `MovementsScreen` — full statement list with all/by-card/by-category filter chips and the period bottom sheet.
   - `AddTransactionScreen` — the manual form UI (amount keypad-first, method Card/Cash toggle, last-**4**-digits field when Card, category picker). Validation present; persistence stubbed.
   - `RemindersScreen` — list of payment reminders (static mock).
   - `ProfileScreen` — user data, registered cards, export button (stubbed), permissions placeholder.
4. **Mock data.** A single `MockData` provider in `core/common` (or a `feature/*/mock` file) feeding realistic COP transactions, categories, and cards through the same domain models the real repositories will later return — so swapping mock → Room is changing the repository binding, nothing else.

Do **not** in this sprint: create Room entities, request `NotificationListenerService`/SMS permissions, integrate Gmail, or implement real export. Stub those behind the domain repository interfaces.

## Conventions

- **Money is `Money(cents: Long)`, always.** Never `Double`, never `Float`, never raw `Int` rupees/pesos. All formatting goes through `MoneyText` / the money formatter. This is non-negotiable — see `ARCHITECTURE.md`.
- **Cards store the last 4 digits.** The manual form asks for 4, not 3.
- Compose screens are stateless; state comes from a `@HiltViewModel` as a `StateFlow<UiState>`. Screens receive state + event lambdas. No business logic in composables.
- No Room types above the data layer; no Compose/Android types in `core/domain`.
- One reusable component per file in `core/designsystem/component`. Screens compose these — do not re-style primitives inline in a screen when a component exists.
- Single source of truth for currency formatting, date formatting, and dispatchers — put them in `core/common`, inject dispatchers.
- Prefer the custom `Canvas` bar chart over a charting library (the styling requirements make a library net-negative here).
- Kotlin official style. Explicit visibility on public API of `core/*`. Meaningful names; no abbreviations in domain types.
- Strings in `strings.xml` (UI is Spanish). Do not hardcode user-facing Spanish text in composables.

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # install on device/emulator
./gradlew testDebugUnitTest    # JVM unit tests (domain, mappers, parser, money)
./gradlew connectedDebugAndroidTest   # Room DAO instrumented tests (later sprints)
./gradlew lint                 # Android lint
```

Run `assembleDebug` and `testDebugUnitTest` before considering a task done.

## Definition of done (per task)

- Compiles (`assembleDebug` green) and unit tests pass.
- Matches the tokens/components in `DESIGN_SYSTEM.md` — no off-palette hex, no ad-hoc fonts, no shadows.
- Respects the layer boundaries in `ARCHITECTURE.md`.
- Money handled as `Money`/minor units throughout.
- New user-facing strings are in `strings.xml`.

## Guardrails

- Don't add dependencies without a clear reason; prefer AndroidX + first-party Compose. If a library is genuinely warranted, note why in the PR/commit.
- Don't introduce a light theme, extra fonts, or new brand colors — the identity is fixed in `DESIGN_SYSTEM.md`.
- Don't auto-commit captured/parsed transactions to the ledger in later sprints — they always route through `pending_transactions` for user review.
- Keep the export format versioned (`schema_version`) and decoupled from Room entities.
- When unsure about scope, prefer the smaller change and leave a `// TODO(sprint-N):` marker rather than pulling future-sprint work forward.
