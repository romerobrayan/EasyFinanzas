package dev.romerobrayan.tinto.feature.dashboard

import dev.romerobrayan.tinto.core.common.MovementUi
import dev.romerobrayan.tinto.core.designsystem.component.ChartBarUi
import dev.romerobrayan.tinto.core.designsystem.component.MonthOption
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Period
import dev.romerobrayan.tinto.core.domain.model.TransactionType

data class DashboardUiState(
    val monthLabel: String = "",
    val monthOptions: List<MonthOption> = emptyList(),
    val selectedMonthKey: String = "",
    val selectedPeriod: Period = Period.MONTH,
    /** Whether the chart + hero aggregate expenses or income. */
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val bars: List<ChartBarUi> = emptyList(),
    val selectedBarIndex: Int = 0,
    val heroAmount: Money = Money.Zero,
    val heroPeriod: Period = Period.MONTH,
    /** Locale-formatted core of the hero label ("julio", "11 jul", "2026"). */
    val heroDateLabel: String = "",
    val comparison: ComparisonUi? = null,
    val preview: List<MovementUi> = emptyList(),
)

/** Spend of the selected bucket vs the previous one (the "12% vs junio" chip). */
data class ComparisonUi(
    val percent: Int,
    val isDecrease: Boolean,
    /** Good news given the type: spending down, or income up — tints green. */
    val isPositiveChange: Boolean,
    val versusPeriod: Period,
    val versusDateLabel: String,
)
