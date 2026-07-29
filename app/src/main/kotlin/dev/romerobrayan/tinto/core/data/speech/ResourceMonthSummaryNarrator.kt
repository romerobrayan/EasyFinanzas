package dev.romerobrayan.tinto.core.data.speech

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.MoneyFormat
import dev.romerobrayan.tinto.core.domain.model.MonthSummary
import dev.romerobrayan.tinto.core.domain.repository.MonthSummaryNarrator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the spoken sentence from `strings.xml`, which is why this lives in data
 * rather than domain — the wording is a resource, and the domain may not read
 * resources.
 *
 * Amounts go through [MoneyFormat.spokenPesos], not [MoneyFormat.format]: the
 * engine reads bare digits as words, whereas the on-screen `$1.842.500` would be
 * read literally.
 */
@Singleton
class ResourceMonthSummaryNarrator @Inject constructor(
    @ApplicationContext private val context: Context,
) : MonthSummaryNarrator {

    override fun narrate(summary: MonthSummary): String {
        val monthName = Dates.monthName(summary.month)

        if (summary.total.isZero) {
            return context.getString(R.string.tts_summary_empty, monthName)
        }

        val sentences = mutableListOf(
            context.getString(
                R.string.tts_summary_total,
                monthName,
                MoneyFormat.spokenPesos(summary.total),
            ),
        )

        summary.comparison?.let { comparison ->
            val previousMonthName = Dates.monthName(comparison.previousMonth)
            sentences += when {
                comparison.percent == 0 ->
                    context.getString(R.string.tts_summary_comparison_equal, previousMonthName)

                comparison.isDecrease -> context.getString(
                    R.string.tts_summary_comparison_down,
                    comparison.percent,
                    previousMonthName,
                )

                else -> context.getString(
                    R.string.tts_summary_comparison_up,
                    comparison.percent,
                    previousMonthName,
                )
            }
        }

        summary.topCategory?.let { topCategory ->
            sentences += context.getString(
                R.string.tts_summary_top_category,
                topCategory.categoryName,
                MoneyFormat.spokenPesos(topCategory.total),
            )
        }

        return sentences.joinToString(separator = " ")
    }
}
