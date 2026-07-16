package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.Fraunces
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors

/** The "Tinto." brand wordmark — Fraunces with a gold full stop. */
@Composable
fun TintoWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 46.sp,
) {
    val gold = LocalTintoColors.current.gold
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.app_name))
            withStyle(SpanStyle(color = gold)) { append(".") }
        },
        modifier = modifier,
        style = TextStyle(
            fontFamily = Fraunces,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
        ),
        color = MaterialTheme.colorScheme.onBackground,
    )
}
