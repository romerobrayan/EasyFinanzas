# Tinto

[![CI](https://github.com/romerobrayan/EasyFinanzas/actions/workflows/ci.yml/badge.svg)](https://github.com/romerobrayan/EasyFinanzas/actions/workflows/ci.yml)

**Tinto** is a native Android personal-finance app with a dark, wine-toned
("Vino Tinto") identity. Offline-first with a Firebase backend, built around
expenses: a bar-chart dashboard, a Nubank-style statement, manual capture,
payment reminders — and, in later sprints, automatic capture from bank
notifications and Gmail.

UI language is Spanish (es-CO); amounts are Colombian pesos handled as
integer minor units end to end.

## Status — Sprint 1: UI shell ✅ · Sprint 1.5: Firebase ✅ · Sprint 2: CRUD ✅

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
| [TASK_SPRINT_3_CAPTURE.md](TASK_SPRINT_3_CAPTURE.md) | The Sprint 3 brief (planning) |
| [TASK_REMINDER_NOTIFICATIONS.md](TASK_REMINDER_NOTIFICATIONS.md) | Backlog brief: local reminder notifications (after Sprint 3) |

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
