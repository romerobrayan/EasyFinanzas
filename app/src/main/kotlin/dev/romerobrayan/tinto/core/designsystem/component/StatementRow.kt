package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.MovementUi
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

/**
 * One statement line: category tile, title/subtitle stack, right-aligned
 * signed amount. Rows are separated by hairlines at the list level — the row
 * itself stays flat. With [onClick] the whole row is tappable (opens the
 * movement detail sheet).
 */
@Composable
fun StatementRow(
    item: MovementUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val type = LocalTintoTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(iconKey = item.categoryIconKey, colorHex = item.categoryColorHex)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = type.body,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.isRecurring) {
                    Spacer(Modifier.width(6.dp))
                    RecurringBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.categoryName} · ${paymentLabel(item)}",
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        MoneyText(amount = item.amount, type = item.type)
    }
}

@Composable
private fun paymentLabel(item: MovementUi): String = when {
    item.isCash -> stringResource(R.string.method_cash)
    item.cardLast4 != null -> stringResource(R.string.card_mask, item.cardLast4)
    else -> stringResource(R.string.add_method_card)
}

@Preview
@Composable
private fun StatementRowPreview() {
    TintoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                StatementRow(
                    item = MovementUi(
                        id = "1",
                        title = "Mercado D1",
                        categoryName = "Mercado",
                        categoryIconKey = "shopping-cart",
                        categoryColorHex = "#9DC97E",
                        cardLast4 = "3092",
                        isCash = false,
                        amount = Money.ofPesos(86_400),
                        type = TransactionType.EXPENSE,
                        isRecurring = false,
                        date = LocalDate(2026, 7, 10),
                        merchant = "Mercado D1",
                        source = TransactionSource.MANUAL,
                    ),
                )
                StatementRow(
                    item = MovementUi(
                        id = "2",
                        title = "YouTube Premium",
                        categoryName = "Servicios",
                        categoryIconKey = "repeat",
                        categoryColorHex = "#C79A6B",
                        cardLast4 = "2481",
                        isCash = false,
                        amount = Money.ofPesos(26_900),
                        type = TransactionType.EXPENSE,
                        isRecurring = true,
                        date = LocalDate(2026, 7, 9),
                        merchant = "YouTube Premium",
                        source = TransactionSource.MANUAL,
                    ),
                )
            }
        }
    }
}
