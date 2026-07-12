# TASK — Sprint 1: UI shell (Tinto)

Task brief for Claude Code. Execute autonomously, in order. This file is the **plan**; the specs it depends on live in the companion docs — read them first and treat them as authoritative:

- `PROJECT_CONTEXT.md` — what Tinto is, scope, roadmap.
- `ARCHITECTURE.md` — layer rules, package structure, domain model, Room/export design.
- `DESIGN_SYSTEM.md` — Vino Tinto tokens, typography, components.

## Objective

Deliver a **navigable, on-brand app shell wired to in-memory mock data**. Five screens, bottom navigation, center FAB. Full design system. **No Room, no automatic capture, no runtime permissions this sprint** — persistence and capture are stubbed behind domain repository interfaces so a later sprint swaps the binding without touching UI.

Done means: the app installs, opens on the dashboard, every bottom-nav destination and the FAB navigate correctly, and every screen renders realistic mock data in the Vino Tinto palette.

---

## 0. Toolchain baseline

Create a new single-module Android app, Kotlin + Compose, using a Gradle **version catalog** (`gradle/libs.versions.toml`).

- `applicationId` / namespace: `dev.romerobrayan.tinto`
- `compileSdk 35`, `targetSdk 35`, `minSdk 26` (per `ARCHITECTURE.md`; bump compileSdk/targetSdk to 36 only if you also verify the toolchain matrix).
- JVM target 17.

> **Versions below are a known-good baseline verified ~July 2026.** Let Gradle/the IDE bump to the latest stable, but keep the **AGP ↔ Kotlin ↔ KSP ↔ Compose** matrix consistent. Two hard rules that differ from older guides:
> 1. **Use KSP, not kapt.** kapt is in maintenance mode / migration is enforced for new projects. Hilt's and (later) Room's compilers are applied via `ksp(...)`.
> 2. **Compose compiler is a Kotlin plugin.** With Kotlin 2.x you apply `org.jetbrains.kotlin.plugin.compose` (version = the Kotlin version). Do **not** set the legacy `composeOptions { kotlinCompilerExtensionVersion = ... }` — that block is obsolete on Kotlin 2.x.

```toml
[versions]
agp = "9.2.0"
kotlin = "2.4.0"
ksp = "2.4.0-2.0.2"            # MUST track the Kotlin version (pattern: <kotlin>-<ksp>)
composeBom = "2026.06.00"
hilt = "2.57"                  # verify latest stable Dagger/Hilt
hiltNavCompose = "1.2.0"
lifecycle = "2.9.0"            # viewmodel-compose + runtime-compose
navigation = "2.9.0"          # navigation-compose (type-safe routes)
coreKtx = "1.15.0"
activityCompose = "1.10.0"
kotlinxSerialization = "1.7.3"
kotlinxDatetime = "0.6.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavCompose" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

> AGP 9.x ships built-in Kotlin support — follow the AGP 9 wizard's plugin conventions; you may not need to apply `kotlin-android` explicitly. Regardless, you must apply the Compose compiler plugin, KSP, Hilt, and serialization plugins. `material-icons-extended` is used as the icon source for `CategoryIcon` (map each category's `iconKey` to an `ImageVector`); swap for a bundled icon set later if desired.

Acceptance: empty app builds (`./gradlew assembleDebug`) and launches a blank `MainActivity` under `TintoTheme`.

---

## Build order

Bottom-up: tokens → components → data contracts → mock → navigation → screens. UI can't look right until the design system exists, and screens can't be wired until the mock repositories exist. Each step ends buildable; commit per step.

### 1. Design system foundation — `core/designsystem/theme`

Goal: the Vino Tinto identity, ready to consume everywhere.

Do: implement `Color.kt` (all tokens + category palette from `DESIGN_SYSTEM.md`), `Type.kt` (bundle Fraunces + Inter under `res/font/`; define the type scale; enable tabular figures where money/aligned numbers appear), `Shape.kt` (radii), `TintoTheme.kt` (the `darkColorScheme` mapping + `MaterialTheme`), and `LocalTintoColors` (a `CompositionLocal` exposing the semantic extras: `expense`, `income`, `accentGold`, category accents). Dark-first; no light theme.

Acceptance: a throwaway `@Preview` shows text/surfaces in the correct palette; `LocalTintoColors.current.expense` resolves.

### 2. Core components — `core/designsystem/component`

Goal: every reusable piece the screens compose, styled per `DESIGN_SYSTEM.md`, each in its own file, each with a `@Preview`.

Do: `MoneyText` (single source of COP formatting: grouped, tabular, signed + colored by `TransactionType`), `CategoryIcon` (accent glyph on tinted tile from `iconKey` + `colorHex`), `TintoBarChart` (custom `Canvas` bar chart: inactive bars `VtPrimaryContainer`, selected bar `VtPrimary` + gold ring, rounded tops, tappable buckets), `PeriodSelector` (Día/Semana/Mes/Año pills), `MonthSelector` (pill + chevron opening the period sheet), `StatementRow` (tile + title/subtitle + right amount, hairline separators), `RecurringBadge`, and `TintoScaffold` + `TintoBottomBar` (5-slot bar with center FAB).

Acceptance: each component renders correctly in isolation via its preview; no off-palette hex, no shadows, no ad-hoc fonts.

### 3. Domain model + repository contracts — `core/domain`

Goal: the shared model the whole app speaks, with no Android/Room/Compose imports.

Do: implement the domain types from `ARCHITECTURE.md` (`Money` value class in minor units, `Transaction`, `Category`, `Card`, `PaymentMethod`, `TransactionType`, `TransactionSource`, `Period`, `Reminder`) plus repository **interfaces** (`TransactionRepository`, `ReminderRepository`, `CardRepository`) exposing `Flow`-returning reads and suspend writes.

Acceptance: `core/domain` compiles with only Kotlin/coroutines/kotlinx-datetime on its classpath.

### 4. Mock data + repository stubs — `core/common` (or `feature/*/mock`)

Goal: realistic data flowing through the real interfaces, so Sprint 2 swaps mock → Room by changing a Hilt binding only.

Do: a `MockData` provider with believable COP transactions (mix of expenses across all categories + income, at least one flagged recurring, card and cash examples with 4-digit cards), seeded categories, and cards. Implement in-memory `*RepositoryImpl` stubs backed by `MutableStateFlow`, and a Hilt module binding the interfaces to these stubs. Add `@HiltAndroidApp` `TintoApplication`.

Acceptance: a stub repository injected into a test ViewModel emits the mock list as a `StateFlow`.

### 5. Navigation scaffold — `MainActivity` + nav

Goal: the app frame and routing.

Do: single `MainActivity` hosting a `NavHost` with type-safe routes for the 5 destinations; `TintoBottomBar` drives navigation; center FAB routes to `AddTransactionScreen`. Each destination initially renders a placeholder composable.

Acceptance: install, tap through all four bottom-nav destinations + the FAB; correct screen shows each time; active-tab color updates.

### 6. Dashboard — `feature/dashboard`

Goal: the hero screen.

Do: `DashboardViewModel` exposes `DashboardUiState` (selected `Period`, chart buckets aggregated from mock transactions via a use case, period total, MoM comparison, recent-movements preview). `DashboardScreen` composes `MonthSelector` + `PeriodSelector` + `TintoBarChart` + a money hero (`moneyHero` style) + MoM chip + a short `StatementRow` preview list. Changing the period updates the chart from state (no Claude round-trip).

Acceptance: matches the validated dashboard mockup; switching Día/Semana/Mes/Año re-buckets the bars; total and preview reflect the selected period.

### 7. Movements — `feature/movements`

Goal: the full statement.

Do: `MovementsScreen` with the complete transaction list (grouped by date), all/by-card/by-category filter chips, and the period bottom sheet (mirrors Nubank's "Selecciona un Extracto"). Recurring items show `RecurringBadge`. Filtering handled in the ViewModel/state.

Acceptance: list renders from mock; filters narrow it; period sheet opens and re-scopes the list.

### 8. Add transaction (manual form) — `feature/addtransaction`

Goal: the fast-capture form UI (persistence stubbed).

Do: `AddTransactionScreen` — amount field keypad-first, `TransactionType` toggle (expense/income), method toggle (Card/Cash), **last-4-digits** field shown only when Card, category picker (from seeded categories), date (default today, editable), optional merchant/description. Client-side validation (amount > 0, category required, last-4 required + 4 digits when Card). On submit, call the stub repository and pop back.

Acceptance: form validates; submitting a mock expense returns to the dashboard/movements and the new item appears in the in-memory list.

### 9. Reminders — `feature/reminders`

Goal: static reminders list.

Do: `RemindersScreen` listing mock payment reminders (title, amount, due date, recurrence, paid state). No scheduling/notifications this sprint.

Acceptance: renders mock reminders on-brand.

### 10. Profile — `feature/profile`

Goal: profile + stubbed export/permissions.

Do: `ProfileScreen` — user data section, registered cards list (bank + `****last4`), an **Export** button (stub: no real file write yet), and a permissions placeholder for the future capture onboarding.

Acceptance: renders; Export button present but inert (leave `// TODO(sprint-2): real JSON export`).

---

## Definition of done (whole sprint)

- `./gradlew assembleDebug` green; `./gradlew testDebugUnitTest` passes; `./gradlew lint` clean of new errors.
- App installs, opens on the dashboard, all navigation works, all screens show mock data.
- Strictly matches `DESIGN_SYSTEM.md`: dark-first Vino Tinto, no off-palette hex, only Fraunces + Inter, two weights, no shadows (fill-layered surfaces + hairlines only).
- Layer boundaries from `ARCHITECTURE.md` respected: no Room/Compose in domain; no persistence types above data; screens stateless + `StateFlow`-driven.
- **Money is `Money`/minor units everywhere**; all currency rendered through `MoneyText`.
- User-facing Spanish strings live in `strings.xml`.

## Out of scope (leave `// TODO(sprint-N:)` markers)

Room entities/DAOs, real JSON export, `NotificationListenerService`/SMS/Gmail capture, runtime permissions, biometric lock, budgets, recurring-charge *detection* logic (the badge is mock-driven this sprint), home-screen widget. Do not pull these forward.

## Suggested commit sequence

`chore: scaffold project + version catalog` → `feat(design): Vino Tinto theme + tokens` → `feat(design): core components` → `feat(domain): model + repository contracts` → `feat(data): in-memory mock repositories` → `feat(nav): scaffold + bottom bar + FAB` → `feat(dashboard): chart + period + hero` → `feat(movements): statement + filters + period sheet` → `feat(add): manual transaction form` → `feat(reminders): list` → `feat(profile): profile + stub export`.
