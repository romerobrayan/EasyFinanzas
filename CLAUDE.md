# CLAUDE.md — Tinto

Operating manual for Claude Code on this repository. Read `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, and `DESIGN_SYSTEM.md` before writing code — they are the source of truth for product scope, structure, and visual identity. This file is the how-we-work layer on top.

## Project

- **Tinto** — a native Android personal-finance app. Dark, wine-toned ("Vino Tinto"). Local-first, no backend yet.
- Package: `dev.romerobrayan.tinto` (rename the suffix if the app name changes).
- Stack: Kotlin, Jetpack Compose, Material 3, Hilt, Room, Navigation Compose, coroutines/Flow.
- Architecture: Clean Architecture + MVVM, package-by-feature. Layer rules are in `ARCHITECTURE.md`.
- `compileSdk 35`, `targetSdk 35`, `minSdk 26`.

## Current sprint — Sprint 1: UI shell

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
