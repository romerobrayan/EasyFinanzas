package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.core.common.MoneyFormat
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * The single way money is rendered in Tinto: grouped COP with tabular
 * figures. With a [type], the amount is signed (− expense / + income) and
 * colored; with null it renders neutral (e.g. the dashboard hero).
 */
@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = LocalTintoTypography.current.moneyRow,
    neutralColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    val tinto = LocalTintoColors.current
    val (sign, color) = when (type) {
        TransactionType.EXPENSE -> "−" to tinto.expense
        TransactionType.INCOME -> "+" to tinto.income
        null -> "" to neutralColor
    }
    Text(
        text = sign + MoneyFormat.format(amount),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
    )
}

@Preview
@Composable
private fun MoneyTextPreview() {
    TintoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                MoneyText(
                    amount = Money.ofPesos(1_842_500),
                    style = LocalTintoTypography.current.moneyHero,
                )
                MoneyText(amount = Money.ofPesos(25_000), type = TransactionType.EXPENSE)
                MoneyText(amount = Money.ofPesos(2_850_000), type = TransactionType.INCOME)
            }
        }
    }
}
