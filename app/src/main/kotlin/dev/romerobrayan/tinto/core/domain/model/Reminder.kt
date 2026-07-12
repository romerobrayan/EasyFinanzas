package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.LocalDate

data class Reminder(
    val id: String,
    val title: String,
    val amount: Money?,
    val dueDate: LocalDate,
    val recurrence: Recurrence,
    val isPaid: Boolean,
)
