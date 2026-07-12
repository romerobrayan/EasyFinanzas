package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.core.domain.model.Money

/** One bar of the spend chart, already labeled for the axis. */
data class ChartBarUi(
    val label: String,
    val value: Money,
)

/**
 * The dashboard hero: a custom Canvas bar chart. Inactive bars are
 * `primaryContainer`; the selected bar is `primary` with a 1.5dp gold ring.
 * Tops are rounded 6dp, axis labels use the meta style, and bars are
 * tappable to drill into that bucket. No chart library — the styling is the
 * point.
 */
@Composable
fun TintoBarChart(
    bars: List<ChartBarUi>,
    selectedIndex: Int,
    onBarSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val tinto = LocalTintoColors.current
    val type = LocalTintoTypography.current
    val textMeasurer = rememberTextMeasurer()

    val growth = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) {
        growth.snapTo(0f)
        growth.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
    val maxCents = remember(bars) {
        (bars.maxOfOrNull { it.value.cents } ?: 0L).coerceAtLeast(1L)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    if (bars.isNotEmpty()) {
                        val slot = size.width / bars.size.toFloat()
                        onBarSelected((offset.x / slot).toInt().coerceIn(0, bars.lastIndex))
                    }
                }
            },
    ) {
        if (bars.isEmpty()) return@Canvas

        val labelZone = 26.dp.toPx()
        val topInset = 6.dp.toPx()
        val baseline = size.height - labelZone
        val slot = size.width / bars.size
        val barWidth = minOf(slot * 0.52f, 36.dp.toPx())
        val corner = 6.dp.toPx()
        val minBarHeight = 3.dp.toPx()

        bars.forEachIndexed { index, bar ->
            val isSelected = index == selectedIndex
            val fullHeight = bar.value.cents.toFloat() / maxCents * (baseline - topInset)
            val barHeight = maxOf(minBarHeight, fullHeight * growth.value)
            val left = index * slot + (slot - barWidth) / 2f
            val barRect = Rect(left, baseline - barHeight, left + barWidth, baseline)

            drawPath(
                path = barRect.topRoundedPath(corner),
                color = if (isSelected) colorScheme.primary else colorScheme.primaryContainer,
            )

            if (isSelected) {
                val inflate = 2.dp.toPx()
                val ringRect = Rect(
                    barRect.left - inflate,
                    barRect.top - inflate,
                    barRect.right + inflate,
                    barRect.bottom,
                )
                drawPath(
                    path = ringRect.topRoundedPath(corner + inflate),
                    color = tinto.gold,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }

            val labelStyle = if (isSelected) {
                type.meta.copy(color = colorScheme.onBackground, fontWeight = FontWeight.Medium)
            } else {
                type.meta.copy(color = tinto.muted)
            }
            val measuredLabel = textMeasurer.measure(bar.label, labelStyle)
            drawText(
                textLayoutResult = measuredLabel,
                topLeft = Offset(
                    x = index * slot + (slot - measuredLabel.size.width) / 2f,
                    y = baseline + (labelZone - measuredLabel.size.height) / 2f,
                ),
            )
        }
    }
}

private fun Rect.topRoundedPath(cornerRadius: Float): Path = Path().apply {
    addRoundRect(
        RoundRect(
            rect = this@topRoundedPath,
            topLeft = CornerRadius(cornerRadius),
            topRight = CornerRadius(cornerRadius),
            bottomRight = CornerRadius.Zero,
            bottomLeft = CornerRadius.Zero,
        ),
    )
}

@Preview
@Composable
private fun TintoBarChartPreview() {
    TintoTheme {
        TintoBarChart(
            bars = listOf(
                ChartBarUi("ene", Money.ofPesos(980_000)),
                ChartBarUi("feb", Money.ofPesos(1_240_000)),
                ChartBarUi("mar", Money.ofPesos(760_000)),
                ChartBarUi("abr", Money.ofPesos(1_100_000)),
                ChartBarUi("may", Money.ofPesos(890_000)),
                ChartBarUi("jun", Money.ofPesos(1_320_000)),
                ChartBarUi("jul", Money.ofPesos(1_045_000)),
            ),
            selectedIndex = 6,
            onBarSelected = {},
        )
    }
}
