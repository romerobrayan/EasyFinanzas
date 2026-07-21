# Tinto

[![CI](https://github.com/romerobrayan/EasyFinanzas/actions/workflows/ci.yml/badge.svg)](https://github.com/romerobrayan/EasyFinanzas/actions/workflows/ci.yml)

**Tinto** is a native Android personal-finance app with a dark, wine-toned
("Vino Tinto") identity. Offline-first with a Firebase backend, built around
expenses: a bar-chart dashboard, a Nubank-style statement, manual capture,
payment reminders — and, in later sprints, automatic capture from bank
notifications and Gmail.

UI language is Spanish (es-CO); amounts are Colombian pesos handled as
integer minor units end to end.

## Status — Sprint 1: UI shell ✅ · Sprint 1.5: Firebase ✅ · Sprint 2: CRUD ✅ · Sprint 3: SMS capture ✅ · Reminder notifications ✅ · Sprint 4 phase 1: Nu notification capture ✅ (Gmail pending) · Sprint 5: categorías + automatización ✅

The complete visual shell plus the Firebase backbone: Google sign-in
(Credential Manager), per-user Cloud Firestore persistence with offline
cache, Analytics and Crashlytics. A demo mode keeps the app fully
explorable with sample data before signing in. **One-time console setup
is required — see [FIREBASE_SETUP.md](FIREBASE_SETUP.md).**

Sprint 2 makes real accounts fully manageable day to day: movement detail
with edit and delete, card management in Perfil, and payment reminders
with create/edit/delete and a recurrence-aware "mark as paid" (one-off
reminders complete; weekly/monthly/yearly ones reschedule). Everything
behaves identically in demo mode and signed in.

Sprint 3 brings automatic capture, SMS first: an opt-in reader for
Bancolombia and 1CERO1 bank messages (live receive + a bounded 90-day
backfill) parsed by data-driven per-issuer rule sets into a device-local
Room staging store. Detected movements surface in a pending inbox —
multi-select with a select-all check, a per-item category picker, and a
"Posible duplicado" badge (duplicates start unselected; the user decides
whether to add or discard them). Nothing reaches the ledger without
explicit confirmation. On top of that, reminders now fire real local
notifications: at `dueTime` (or 08:00 for date-only reminders), surviving
reboots, in demo and signed-in mode alike — all on-device, no server.

Sprint 4 phase 1 lights up the second capture channel: Nu push
notifications. A `NotificationListenerService` — behind its own opt-in in
Perfil plus the system notification-access grant — feeds Nu's purchase and
payment notifications through the same on-device parser and pending inbox
as SMS, with relative dates falling back to the notification's post time
and payment *requests* dropped as noise. No history backfill exists for
notifications: capture starts from the moment access is granted. Phase 2
(Gmail transactional email) is still pending.

Sprint 5 splits categories by direction and adds automation. Expenses gain
**Hogar** and **Emergencias**; incomes get their own set — Nómina, Pago
deuda, Préstamo, Movimiento, Aportes — with the add/edit form showing only
the categories that match the selected type (existing accounts pick up the
new system categories through an idempotent backfill). Income mode also gets
its own fields: method is Efectivo / Transferencia / a registered card
(chosen by tap, no 4-digit prompt). And any movement can be **automated** —
a switch reveals a frequency (Diario / Semanal / **Quincenal** / Mensual)
and the app materializes the recurring movements straight into the list,
marked *recurrente*. "Quincenal" is Colombian payroll cadence: the 15th and
the last day of each month. A catch-up generator with deterministic ids
fills in anything missed since the app last ran without ever duplicating,
and a "Movimientos automáticos" manager in Perfil lists the rules with
pause/resume and delete. Income by Nu notification is deferred to Sprint 6;
in this sprint incomes arrive from the manual form and Bancolombia SMS.

## Stack

Kotlin · Jetpack Compose (Material 3) · Hilt · Navigation Compose
(type-safe routes) · coroutines/Flow · kotlinx-datetime · Firebase
(Auth + Google sign-in, Cloud Firestore, Analytics, Crashlytics) —
AGP 9.x with built-in Kotlin and KSP2, single module, Clean
Architecture + MVVM, package by feature.

## Project docs

| Doc | What it is |
|---|---|
| [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) | Product scope, personas, roadmap |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layers, domain model, Room/export design |
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | Vino Tinto tokens, type scale, components |
| [FIREBASE_SETUP.md](FIREBASE_SETUP.md) | One-time Firebase console setup + troubleshooting |
| [CLAUDE.md](CLAUDE.md) | Working agreements for AI-assisted development |
| [TASK_SPRINT_1_UI_SHELL.md](TASK_SPRINT_1_UI_SHELL.md) | The Sprint 1 brief |
| [TASK_SPRINT_2_CRUD.md](TASK_SPRINT_2_CRUD.md) | The Sprint 2 brief |
| [TASK_SPRINT_3_CAPTURE.md](TASK_SPRINT_3_CAPTURE.md) | The Sprint 3 brief |
| [TASK_REMINDER_NOTIFICATIONS.md](TASK_REMINDER_NOTIFICATIONS.md) | Brief: local reminder notifications (delivered after Sprint 3) |
| [TASK_SPRINT_4_NOTIFICATIONS_GMAIL.md](TASK_SPRINT_4_NOTIFICATIONS_GMAIL.md) | The Sprint 4 brief (Nu notifications + Gmail) |

## Build

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install on a device/emulator
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew lint                 # Android lint
```

Requires JDK 17+. Every push runs the same three tasks on CI and uploads
the debug APK as a workflow artifact (Actions → latest run →
`tinto-debug-apk`).
