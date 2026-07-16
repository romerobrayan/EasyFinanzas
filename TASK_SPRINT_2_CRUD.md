# TASK_SPRINT_2_CRUD.md — Sprint 2: real-account CRUD

## Where the app stands (context for this sprint)

Sprint 1 (UI shell) and Sprint 1.5 (Firebase) are **done, verified on a real
device, and live on `main`**:

- Session gate in `navigation/TintoRoot.kt`: `Loading → LoginScreen (Google via
  Credential Manager) → Demo | SignedIn (TintoApp)`.
- Data routes by session through the `Synced*Repository` classes in
  `core/data/repository/`: Cloud Firestore under `users/{uid}/…` when signed in
  (offline cache on), the `InMemory*` sample repositories in demo mode.
- Firestore document mappers live in `core/data/firebase/FirestoreMappers.kt`.
  **Field names are a persisted schema — do not rename them.**
- Analytics via `core/common/TintoAnalytics` (never log amounts/merchants/PII).
- CI builds, tests, lints and publishes the debug APK to the `ci-apk` branch
  from `main` and `claude/**` pushes. Shared debug keystore `app/debug.keystore`
  — do not touch it, the SHA-1 is registered in Firebase.

The gap: the ledger is **add-only** and real accounts have no way to manage
cards or reminders. Sprint 2 closes exactly that.

## Goal

A signed-in user can run their real finances day to day: fix or remove a
mistyped movement, register their actual cards, and manage payment reminders.
Everything works identically in demo mode (in-memory) and signed-in mode
(Firestore), same as today.

## Workstream A — movement detail, edit, delete (highest value)

**Contract** (`core/domain/repository/TransactionRepository.kt`):

```kotlin
suspend fun updateTransaction(transaction: Transaction)
suspend fun deleteTransaction(transactionId: String)
```

- Firestore: `set()` the full mapped document at
  `users/{uid}/transactions/{id}` (fire-and-forget like `addTransaction` —
  never await, offline writes must not hang); `delete()` for removal.
- InMemory: replace/remove in the `MutableStateFlow` list.
- `Synced*` routes by session exactly like `addTransaction` does today.

**UI:**

1. Tapping a `StatementRow` (movements list **and** dashboard preview) opens a
   **detail bottom sheet** (`SheetShape`, on-palette): amount as `MoneyText`
   hero, category with `CategoryIcon`, method (card mask via existing
   `card_mask` string / "Efectivo"), date, optional merchant, source. Two
   actions: **Editar** and **Eliminar**.
2. **Editar** navigates to the existing add-transaction screen prefilled.
   Change `AddTransactionRoute` to `data class AddTransactionRoute(val transactionId: String? = null)`
   (type-safe nav already set up for this). When editing: title
   "Editar movimiento", save button "Guardar cambios", keep the original
   `createdAt` and `source`, set `updatedAt = now`.
3. **Eliminar** shows a confirm `AlertDialog` (strings below) then deletes and
   closes the sheet.

**Analytics:** `edit_transaction` and `delete_transaction` (params: type +
method only).

## Workstream B — card management in Perfil

**Contract** (`CardRepository`): `addCard`, `updateCard`, `deleteCard(cardId)`.

**UI:** in the existing "Tarjetas" section of `ProfileScreen`:

- An "Agregar tarjeta" row (+ icon, same row style as the export button).
- A bottom-sheet form: **banco** (required text), **últimos 4 dígitos**
  (required, exactly 4 digits — same rule as the add-transaction form),
  **etiqueta** optional (e.g. "Débito"/"Crédito"). New id = `UUID.randomUUID()`.
- Tapping an existing card opens the same sheet prefilled + an "Eliminar
  tarjeta" action (confirm dialog).
- Deleting a card must **not** touch existing transactions: rows referencing a
  missing `cardId` already render as unmatched — that behavior stays.
- After saving a card, the add-transaction last-4 matching and the by-card
  filters pick it up automatically (they observe `CardRepository`).

**Analytics:** `add_card`, `delete_card` (no bank names, no digits).

## Workstream C — reminders CRUD

**Contract** (`ReminderRepository`): `addReminder`, `updateReminder`,
`deleteReminder(reminderId)`.

**UI** (`RemindersScreen`):

- Header action "Agregar" (text button, gold) → bottom-sheet form: **título**
  (required), **monto** optional (peso digits → `Money.ofPesos`), **fecha de
  vencimiento** (reuse the date picker pattern from add-transaction),
  **recurrencia** chips (Única vez / Semanal / Mensual / Anual).
- Row tap → same sheet prefilled + "Eliminar" (confirm).
- **Marcar como pagado**, with recurrence-aware behavior:
  - `NONE` → set `isPaid = true` (moves to the "Pagados" section).
  - `WEEKLY/MONTHLY/YEARLY` → advance `dueDate` by one period and keep
    `isPaid = false` (it stays in "Próximos" with the new date). Put this rule
    in a small pure function in `core/domain` and **unit test it** (including
    month-end edge cases — `kotlinx.datetime` `plus(1, DateTimeUnit.MONTH)`
    clamps day-of-month; that clamping behavior is acceptable, just pin it in
    the test).

**Analytics:** `add_reminder`, `reminder_paid` (no titles/amounts).

## Cross-cutting rules

- Demo mode gets the same CRUD (update the `InMemory*` repositories); nothing
  persists there, same as today.
- No Firestore schema changes: same field names, `Money` as cents `Long`,
  `Instant` as epoch millis, `LocalDate` as ISO string. If you add a mapper,
  add a `FirestoreMappersTest` case pinning the field names.
- All new user-facing strings in `res/values/strings.xml`, Spanish, following
  the existing naming (`movement_detail_*`, `card_form_*`, `reminder_form_*`,
  `action_delete`, …).
- Stateless composables + `@HiltViewModel` state, per `CLAUDE.md`. Bottom
  sheets follow `MonthPickerSheet`'s style (shape/colors/typography tokens).
- Money is `Money(cents: Long)` everywhere. Cards keep **4** digits.

## Out of scope (do not pull forward)

Automatic capture (notifications/SMS/Gmail), JSON export, category management,
budgets, light theme. Leave `// TODO(sprint-3):` markers where natural.

## Definition of done

- `./gradlew assembleDebug testDebugUnitTest lint` green locally-on-CI (the
  container can't reach Google's Maven — push and let CI verify, iterate until
  green).
- New unit tests: reminder recurrence rollover + any new validator logic.
- Works in demo mode and signed-in mode; offline add/edit/delete syncs when
  connectivity returns (Firestore cache — no extra code, just don't `await`
  writes).
- Docs touched if behavior changed (`README.md` status line, `CLAUDE.md`
  sprint section).
- Landed on `main` (fast-forward from the working branch after CI is green),
  APK republished on `ci-apk`.
