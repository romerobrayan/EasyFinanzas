# TASK_REMINDER_NOTIFICATIONS.md — Backlog: local reminder notifications

> **Scheduled after Sprint 3 (automatic capture).** Do not pull this forward —
> capture ships first. This brief exists so the decision context isn't lost.

## Where the app stands (context for this task)

Sprint 2 delivered reminders CRUD with an **optional time of day**:
`Reminder.dueTime: LocalTime?` (Firestore field `dueTime`, `"HH:MM"` string,
additive), picked via `TintoTimePickerDialog` in `ReminderFormSheet`, shown in
the list label, and preserved through the recurrence rollover
(`core/domain/usecase/ReminderRollover.kt`). **The time is stored and displayed
but nothing fires** — the app never schedules an Android notification, has no
channel, and never requests the notification permission. The "Notificaciones
bancarias" row in Perfil is unrelated (that is Sprint 3's capture of *incoming*
bank notifications, not reminder alerts).

## Goal

A reminder with a due date (and optionally a time) produces a local
notification on the device at the right moment, in demo mode and signed-in mode
alike, surviving reboots. Everything on-device — **no FCM, no server**.

## Workstreams

### A — Permission + channel

- One notification channel, id `reminders`, user-visible name/description in
  Spanish (`strings.xml`: `notif_channel_reminders`, …). Created at app start.
- `POST_NOTIFICATIONS` runtime permission (Android 13+; `minSdk 26` means it
  must be requested conditionally). Ask **in context**: the first time a user
  saves a reminder, not at app launch. A soft explainer + system prompt;
  declining degrades gracefully (reminders still work, no alert).
- Replace the "Próximamente" placeholder row in Perfil with the real permission
  state for notifications once this lands (align with however Sprint 3 left
  that section).

### B — Scheduling

- Trigger instant = `dueDate` at `dueTime`, or at a default hour when
  `dueTime == null` (default **8:00**, constant in `core/domain`). Compute in a
  **pure function** (`nextTriggerInstant(reminder, timeZone)` or similar) in
  `core/domain` and unit test it (date-only default hour, past-due → no
  trigger, timezone edges).
- Prefer `AlarmManager.setAndAllowWhileIdle` (inexact is fine — a payment
  reminder tolerates minutes of drift) over exact alarms, so we do not need the
  Android 12+ `SCHEDULE_EXACT_ALARM` special access. WorkManager is an
  acceptable alternative if Doze behavior proves unreliable.
- One pending intent per reminder id (request code from the id hash) so
  reschedule/cancel is idempotent.

### C — Lifecycle wiring

- A `ReminderScheduler` seam observing the bound `ReminderRepository` (a small
  data-layer service started from the session gate): on every emission,
  reconcile alarms — schedule unpaid future reminders, cancel paid/deleted
  ones. Observing the repository (not hooking each CRUD call) makes demo and
  signed-in behave identically for free, covers the recurrence rollover
  (mark-paid updates `dueDate` → next occurrence gets scheduled), and covers
  multi-device edits arriving via Firestore sync.
- `BOOT_COMPLETED` receiver re-registers everything (alarms die on reboot).
  Also handle `TIMEZONE_CHANGED`/`TIME_SET`.
- Signed-out: cancel all scheduled alarms.

### D — The notification itself

- Content: reminder title + amount when present (`MoneyFormat`), e.g.
  "Pago tarjeta Nu — $412.000 vence hoy". On-device only, so showing the title
  is fine — but keep **analytics** clean per `TintoAnalytics` rules: an event
  like `reminder_notification_shown` carries recurrence at most, never
  titles/amounts.
- Tap opens the app on the Recordatorios tab (intent extra handled in
  `MainActivity`/`TintoApp`).
- Small icon: monochrome variant of the launcher glyph, on-palette accent color.

## Out of scope

Snooze actions, per-reminder lead times ("avísame 2 días antes"), FCM/server
push, calendar integration. Leave `// TODO:` markers where natural.

## Definition of done

- `./gradlew assembleDebug testDebugUnitTest lint` green on CI.
- Unit tests: trigger-instant computation (default hour, past dates, rollover
  interaction).
- Manual check on device: reminder with time fires at that time; date-only
  fires at 08:00; mark-paid recurring reminder fires again next period; reboot
  does not lose alarms; demo mode behaves the same.
- Docs updated (`README.md` status, `CLAUDE.md` sprint sections).
