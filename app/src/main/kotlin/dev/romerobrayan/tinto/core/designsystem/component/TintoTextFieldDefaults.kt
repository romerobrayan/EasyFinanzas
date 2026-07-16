package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors

/**
 * The one way Tinto styles filled text fields: surface-variant container,
 * no indicator line, gold cursor, muted labels. Every form (add movement,
 * card, reminder) uses these colors so inputs read identically app-wide.
 */
@Composable
fun tintoTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    cursorColor = LocalTintoColors.current.gold,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedLabelColor = LocalTintoColors.current.muted,
    unfocusedLabelColor = LocalTintoColors.current.muted,
)
