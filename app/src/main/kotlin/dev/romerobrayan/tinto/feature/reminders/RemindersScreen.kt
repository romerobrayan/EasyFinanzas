package dev.romerobrayan.tinto.feature.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography

// Placeholder — replaced by the reminders list in the reminders commit.
@Composable
fun RemindersScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.reminders_title),
            style = LocalTintoTypography.current.screenTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
