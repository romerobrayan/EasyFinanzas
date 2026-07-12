# PROJECT_CONTEXT.md — Tinto

> Working name: **Tinto** (Colombian double meaning: black coffee + red wine, tied to the "Vino Tinto" visual identity). Rename freely — it only appears as the app label and package suffix.

## What this is

A personal Android app for tracking personal finances, built by a single developer as a portfolio-grade project. The app centers on **expenses**: the home screen is a dashboard whose hero element is a bar chart of spending, filterable by day / week / month / year. Below it sits a transaction history styled like a bank statement (the reference is Nubank's "Extractos" screen).

Data enters the app three ways:

1. **Manual** — a form the user fills in (amount, source, category, etc.).
2. **Automatic — device notifications** — reading bank/fintech push notifications (Bancolombia, Nequi, Daviplata, etc.) via a `NotificationListenerService`.
3. **Automatic — email** — parsing transactional emails from a linked Gmail account.

Storage is **local-first** (Room / SQLite). There is no backend yet. The app can **export** its data as versioned JSON so that a future "dashboard API" can consume it. That export contract is a first-class design concern, not an afterthought — see `ARCHITECTURE.md`.

## Why these technical choices

- **Kotlin + Jetpack Compose (native), not Flutter.** The differentiating feature — automatic capture — lives entirely in native Android territory (`NotificationListenerService`, the Telephony SMS `ContentProvider`). In Flutter this would be written in Kotlin anyway via platform channels, paying the cost of both stacks. Going native removes that tax.
- **iOS is a future goal, and this choice does not block it.** iOS forbids reading SMS or notifications under any circumstances. The real path to cross-platform is the future sync API distributing normalized data — not a shared UI framework. When iOS arrives, it will be a separate native client talking to the same API contract.
- **Money is stored as `Long` in minor units (cents), never `Double`.** Colombian peso amounts like `$1.842.500` accumulate rounding error under floating-point math. This is the number-one class of bug in finance software and is avoided by design.

## Users / personas

This is a single-user app, but two mental modes drive the UX:

- **"Fin de mes" persona (reflective mode):** wants to *understand* where the money went. Cares about the monthly bar, category breakdown, the "gasto hormiga" total, and month-over-month comparison. Optimize the dashboard for this.
- **"En caja" persona (capture mode):** just paid for something and wants to log it in under five seconds, one-handed, without friction. Optimize the manual add form and the FAB for this — smart defaults, minimal required fields, numeric keypad first.

## Feature scope

### Core (v1)

- Dashboard with expense bar chart + period selector (day / week / month / year).
- Statement-style transaction history with filters (all / by card / by category).
- Manual add form for both expenses and income.
- Payment reminders section.
- Income tracking.
- Profile section (user data, registered cards, permissions, export).
- Local persistence (Room).
- Versioned JSON export.

### High-value additions (proposed, prioritized)

1. **Recurring subscription detection.** The user's own statement already shows two recurring charges (Google One, YouTube). Detecting and surfacing these is the highest-leverage feature — the data is begging for it.
2. **Per-category budgets** with a progress bar and over-budget warning.
3. **"Gasto hormiga" monthly insight** — a highlighted card summing small recurring discretionary spend ("this month your ants add up to $X").
4. **Month-over-month comparison** on the dashboard (already teased in the mockup as "12% vs junio").
5. **Biometric lock** (`BiometricPrompt`) gating app open.
6. **Home-screen widget** showing current-month spend.

## Categories (v1 seed set)

Comida, Entretenimiento, Gasto hormiga, Salud, Pasajes / Transporte, Servicios / Suscripciones, Mercado, Otros. Categories are user-extensible; seed these on first run.

## Roadmap (SCRUM sprints)

The work is sequenced so the visual shell exists first, then data, then the harder capture pipeline.

- **Sprint 1 — UI shell (this sprint).** All five screens + navigation + FAB, wired to **in-memory mock data**. Full design system (theme, tokens, reusable components). No Room, no capture. Deliverable: a navigable, on-brand app you can click through. This is what Claude Code builds first.
- **Sprint 2 — Persistence.** Room entities/DAOs/repositories, the manual add form fully functional, versioned JSON export via the Storage Access Framework.
- **Sprint 3 — Automatic capture.** `NotificationListenerService`, the parser, and the pending-transaction review queue. (Message-filtering rules are designed here, once the backend/capture discussion happens.)
- **Sprint 4 — Gmail + reminders.** Gmail transactional-email parsing and functional payment reminders with local notifications.

## Out of scope (for now)

Backend API, multi-user, cloud sync, iOS client, direct bank API integrations. All deferred behind the export contract.
