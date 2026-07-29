package dev.romerobrayan.tinto.core.domain.model

/**
 * How often an automated movement repeats (Sprint 5). Deliberately separate
 * from [Recurrence] (reminders): [SEMIMONTHLY] ("quincenal") is Colombian
 * payroll cadence — the 15th and the last day of each month, fixed calendar
 * dates rather than every-N-days.
 *
 * UI labels: DAILY = Diario, WEEKLY = Semanal, SEMIMONTHLY = Quincenal,
 * MONTHLY = Mensual.
 */
enum class TransactionFrequency { DAILY, WEEKLY, SEMIMONTHLY, MONTHLY }
