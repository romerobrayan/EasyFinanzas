# ARCHITECTURE.md — Tinto

## Stack

- Language: Kotlin (latest stable), coroutines + Flow.
- UI: Jetpack Compose, Material 3 (`androidx.compose.material3`).
- Architecture: Clean Architecture + MVVM.
- DI: Hilt.
- Persistence: routed by session. A signed-in account lives in Cloud Firestore, one subtree per account (`users/{uid}/…`), with the SDK's offline cache so the app works without connectivity. A **no-account (local) profile** keeps its whole ledger in Room on the device and never touches the network — see "Sessions and persistence" below. Auth: Firebase Auth (Google, via Credential Manager) or a nickname stored in SharedPreferences for local profiles. The Room staging layer landed in Sprint 3: a device-local `pending_transactions` table holding captured parses until the user confirms or discards them.
- Backend services: Firebase Analytics + Crashlytics behind the `TintoAnalytics` interface in `core/common`.
- Navigation: Navigation Compose (type-safe routes).
- Charts: Compose-native custom chart (draw with `Canvas` / `drawScope`) — do **not** pull a heavy third-party chart lib for a simple bar chart; the design demands custom styling anyway. Vico is an acceptable fallback only if the custom implementation becomes a time sink.
- SDK: `compileSdk 35`, `targetSdk 35`, `minSdk 26` (Android 8.0 — required for reliable `NotificationListenerService`, adaptive icons, and it covers the large majority of active devices).

## Layered structure

Three layers, package-by-feature at the top, clean layering inside `core` and each feature.

```
dev.romerobrayan.tinto
├── TintoApplication.kt            (@HiltAndroidApp)
├── MainActivity.kt                (single activity, hosts NavHost)
├── core
│   ├── designsystem               theme, tokens, reusable composables (see DESIGN_SYSTEM.md)
│   │   ├── theme                  Color.kt, Type.kt, Shape.kt, TintoTheme.kt
│   │   └── component              TintoBarChart, StatementRow, CategoryIcon, MoneyText, PeriodSelector, TintoScaffold …
│   ├── domain                     cross-feature domain model + contracts
│   │   ├── model                  Transaction, Money, Category, Card, PaymentSource, Reminder, Period
│   │   └── repository             TransactionRepository, ReminderRepository, CardRepository  (interfaces)
│   ├── data                       cross-feature data layer
│   │   ├── local                  TintoDatabase, DAOs, @Entity classes, TypeConverters
│   │   ├── repository             *RepositoryImpl
│   │   ├── mapper                 Entity <-> domain mappers
│   │   └── capture                notification + email capture sources  (Sprint 3+)
│   ├── common                     Result wrapper, money formatting, date utils, dispatchers
│   └── di                         Hilt modules (DatabaseModule, RepositoryModule, DispatcherModule)
└── feature
    ├── dashboard                  DashboardScreen, DashboardViewModel, DashboardUiState
    ├── movements                  MovementsScreen (statement), filters, period bottom sheet
    ├── addtransaction             AddTransactionScreen (manual form), validation
    ├── reminders                  RemindersScreen                              (Sprint 4)
    └── profile                    ProfileScreen (user data, cards, export)
```

### Layer rules

- **presentation** (`feature/*`): Compose screens are stateless and driven by a `UiState` exposed as `StateFlow` from a `@HiltViewModel`. Screens emit events up; ViewModels call use cases (or repositories directly for trivial reads). Never reference persistence types in this layer. One deliberate exception to "no platform calls in composables": the Credential Manager account picker (`feature/login/GoogleCredential.kt`) needs the Activity context, so the login screen fetches the Google ID token and hands it to the ViewModel.
- **domain** (`core/domain`): pure Kotlin, no Android/Firebase/Compose imports. Holds the domain model, repository interfaces (including `AuthRepository` + `UserSession`), and use cases. Money math lives here.
- **data** (`core/data`): the `Synced*Repository` implementations route by session — signed-in traffic goes to `users/{uid}/…` documents, a local profile to the `Local*Repository` classes over Room, demo mode to the `InMemory*` sample repositories. Manual mappers (`core/data/firebase/FirestoreMappers.kt`, `core/data/local/LedgerEntities.kt`) convert documents/rows to domain models at the repository boundary so domain never leaks persistence details; their field and column names are a persisted schema.

## Sessions and persistence

`UserSession` has four states and every repository routes on it, which is why a feature never knows where its data lives:

| Session | Ledger | Survives restart | Leaves the device |
|---|---|---|---|
| `SignedIn` | Firestore `users/{uid}/…` | yes | yes (that's the point) |
| `Local` | Room on this device | yes | only via JSON export/import |
| `Demo` | `InMemory*` sample data | no | no |
| `SignedOut` / `Loading` | empty flows | — | — |

A `Local` profile is identified by a nickname in SharedPreferences (`core/data/auth/LocalProfileStore`) — no uid, no Firestore listener, and analytics + crash collection are switched off for the whole session via `TintoAnalytics.setCollectionEnabled(false)`. Moving that data to another device is deliberately export → import; there is no sync path, and adding one would mean adding an account.

Use cases are warranted where logic is non-trivial (aggregating spend into chart buckets, detecting recurrence, deduping captured transactions). Trivial CRUD may go straight ViewModel → repository.

## Domain model (key types)

```kotlin
// Money is ALWAYS minor units (centavos). Never Double anywhere in the app.
@JvmInline
value class Money(val cents: Long) {
    operator fun plus(o: Money) = Money(cents + o.cents)
    operator fun minus(o: Money) = Money(cents - o.cents)
}

enum class TransactionType { EXPENSE, INCOME }

enum class PaymentMethod { CARD, CASH }

data class Card(
    val id: String,          // UUID
    val bank: String,
    val last4: String,       // last 4 digits — see note below
    val label: String?,
)

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,     // maps to a Tabler-style icon in the design system
    val colorHex: String,    // category accent (see DESIGN_SYSTEM.md category palette)
    val isSystem: Boolean,   // seeded defaults vs user-created
)

enum class TransactionSource { MANUAL, NOTIFICATION, EMAIL, SMS }

data class Transaction(
    val id: String,          // UUID
    val type: TransactionType,
    val amount: Money,
    val method: PaymentMethod,
    val cardId: String?,     // null when CASH
    val bank: String?,
    val categoryId: String,
    val merchant: String?,   // free-text description / merchant
    val occurredAt: Instant, // when the transaction happened
    val source: TransactionSource,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class Period { DAY, WEEK, MONTH, YEAR }
```

> **Last 4, not 3, digits.** Colombian bank notifications and SMS reference the card as `****1234` (four digits). Storing four is what lets the parser auto-match a captured transaction to a registered `Card`. Storing three would break that match — the whole point of automatic capture. The manual form must therefore ask for the last **4** digits when method = CARD.

## Room schema

Entities mirror the domain but store primitives, mapped by hand at the repository boundary (no `TypeConverter`s): Money → `INTEGER` (Long cents), Instants → `INTEGER` epoch millis, `LocalDate`/`LocalTime` → ISO `TEXT`, enums → their name as `TEXT`.

- `transactions` — one row per movement of a **local profile**. Indexed on `occurredAtEpochMs` (chart/statement queries are date-ranged) and on `categoryId`.
- `cards` — registered cards (`id`, `bank`, `last4`, `label`).
- `reminders` — `id`, `title`, `amountCents?`, `dueDate`, `dueTime?`, `recurrence` (NONE / MONTHLY / …), `isPaid`.
- `recurring_rules` — automations of a local profile, including `nextOccurrence` so the catch-up generator is idempotent.
- `pending_transactions` — **capture staging** (Sprint 3), for every session. Captured items land here first for user review; on confirm they are promoted to the ledger. Never auto-commit a parsed notification straight to the ledger — a bad parse would silently corrupt the user's data.

Categories have no table: the system set is fixed app data with no write API, so it is served from `MockData` (and seeded into Firestore for signed-in accounts).

Schema versions: 1–2 held only `pending_transactions`, so `DatabaseModule` may still recreate those destructively. Version 3 introduced the local ledger — rows with no copy anywhere — so from there on **every change needs a real `Migration`**; schemas are exported to `app/schemas/` to diff against.

DAOs expose `Flow<List<…>>` for observed reads. The dashboard queries aggregate spend grouped by a date bucket derived from the selected `Period`; do the bucketing in a use case over a date-ranged query rather than in SQL date functions, so the logic stays testable and locale-safe.

## Data-capture pipeline (Sprint 3+)

```
NotificationListenerService / SMS ContentProvider / Gmail
        │  raw payload (text)
        ▼
   CaptureSource            (core/data/capture) — normalizes to RawCapture
        │
        ▼
   TransactionParser        extracts amount, bank, last4, direction (debit/credit)
        │                   using per-issuer regex rule sets (Bancolombia, Nequi, …)
        ▼
   pending_transactions     (Room) — staged, awaiting user confirmation
        │  user reviews + assigns category
        ▼
   transactions             (Room) — committed ledger
```

Notes:
- Parser rule sets are data, not code branches — keep them in a structured, extensible form so adding a new bank is adding a rule, not editing a `when`. (Detailed message-filtering rules are designed in the backend/capture discussion, per the roadmap.)
- Dedup: a manual entry and a captured entry for the same charge must be reconcilable. Match on `(amount, last4, occurredAt within a window)`; surface likely duplicates in the review queue rather than silently merging.
- Permissions: `NotificationListenerService` requires the user to grant access in system settings — build a clear onboarding explainer. SMS (`READ_SMS`) is Play-restricted; fine for a personally-sideloaded APK, but if this is ever published, notifications are the sustainable path and SMS should be treated as optional.

## Export contract (local → future API)

Export is a **versioned** JSON document written via the Storage Access Framework (user picks the destination; no broad storage permission needed).

```json
{
  "schema_version": 1,
  "exported_at": "2026-07-11T16:00:00Z",
  "app_version": "1.0.0",
  "currency": "COP",
  "cards": [ … ],
  "categories": [ … ],
  "transactions": [ … ],
  "reminders": [ … ]
}
```

`schema_version` makes the export a stable contract: when the local model evolves, the future dashboard API can migrate by version instead of guessing. Amounts in the export are also integer minor units — the consumer formats for display. Design the serializer as a mapper from domain → export DTOs (kotlinx.serialization), decoupled from Room entities, so persistence changes don't silently alter the wire format.

## Testing posture

- Domain use cases: plain JUnit (money math, chart bucketing, recurrence detection, dedup) — pure functions, no Android.
- DAOs: Room in-memory instrumented tests for queries and converters.
- Parser: table-driven unit tests over real sample notification strings per issuer.
- ViewModels: turbine over the `UiState` flow.
