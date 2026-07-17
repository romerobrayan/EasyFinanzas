package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Reminder(
    val id: String,
    val title: String,
    val amount: Money?,
    val dueDate: LocalDate,
    /** Optional time of day for the reminder; null = date only. */
    val dueTime: LocalTime? = null,
    val recurrence: Recurrence,
    val isPaid: Boolean,
)
