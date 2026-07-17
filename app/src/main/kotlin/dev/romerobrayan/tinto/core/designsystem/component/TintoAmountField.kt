package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.MoneyFormat
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.domain.model.Money

/**
 * Keypad-first amount entry rendered through the app money format. The manual
 * add/edit form and the pending review sheet share it, so amounts are typed
 * the same way everywhere.
 */
@Composable
fun TintoAmountField(
    amountDigits: String,
    onAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    val focusRequester = remember { FocusRequester() }
    val formatted = if (amountDigits.isEmpty()) {
        ""
    } else {
        MoneyFormat.format(Money.ofPesos(amountDigits.toLong()))
    }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = TextFieldValue(text = formatted, selection = TextRange(formatted.length)),
        onValueChange = { onAmountChanged(it.text) },
        textStyle = type.moneyHero.copy(
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(tinto.gold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (formatted.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_amount_placeholder),
                        style = type.moneyHero,
                        color = tinto.muted,
                    )
                }
                innerTextField()
            }
        },
    )
}
