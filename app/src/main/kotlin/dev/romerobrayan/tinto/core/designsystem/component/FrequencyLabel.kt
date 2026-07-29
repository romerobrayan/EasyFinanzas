package dev.romerobrayan.tinto.core.designsystem.component

import androidx.annotation.StringRes
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency

/** UI label for an automation frequency. Single source for form + management screen. */
@StringRes
fun frequencyLabelRes(frequency: TransactionFrequency): Int = when (frequency) {
    TransactionFrequency.DAILY -> R.string.frequency_daily
    TransactionFrequency.WEEKLY -> R.string.frequency_weekly
    TransactionFrequency.SEMIMONTHLY -> R.string.frequency_semimonthly
    TransactionFrequency.MONTHLY -> R.string.frequency_monthly
}
