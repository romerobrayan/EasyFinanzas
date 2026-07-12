package dev.romerobrayan.tinto.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.FabShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme

/** The four navigable tabs; the center slot belongs to the add FAB. */
enum class TintoTab(val icon: ImageVector, @field:StringRes val labelRes: Int) {
    HOME(Icons.Outlined.Home, R.string.nav_home),
    MOVEMENTS(Icons.Outlined.ReceiptLong, R.string.nav_movements),
    REMINDERS(Icons.Outlined.Notifications, R.string.nav_reminders),
    PROFILE(Icons.Outlined.Person, R.string.nav_profile),
}

/**
 * App frame: background-colored Scaffold plus the 5-slot bottom bar
 * (4 tabs + center FAB). Pass a null [currentTab] to hide the bar
 * (e.g. on the add-transaction screen).
 */
@Composable
fun TintoScaffold(
    currentTab: TintoTab?,
    onTabSelected: (TintoTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentTab != null) {
                TintoBottomBar(
                    selected = currentTab,
                    onTabSelected = onTabSelected,
                    onAddClick = onAddClick,
                )
            }
        },
        content = content,
    )
}

private val FabRingSize = 64.dp
private val FabProtrusion = 24.dp

@Composable
fun TintoBottomBar(
    selected: TintoTab,
    onTabSelected: (TintoTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = FabProtrusion),
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabItem(TintoTab.HOME, selected, onTabSelected, Modifier.weight(1f))
                    TabItem(TintoTab.MOVEMENTS, selected, onTabSelected, Modifier.weight(1f))
                    Spacer(Modifier.width(FabRingSize + 12.dp))
                    TabItem(TintoTab.REMINDERS, selected, onTabSelected, Modifier.weight(1f))
                    TabItem(TintoTab.PROFILE, selected, onTabSelected, Modifier.weight(1f))
                }
            }
        }
        // The FAB pokes above the bar; the 4dp surface ring is the knockout
        // where it overlaps — depth by fill color, never by shadow.
        FabWithRing(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun TabItem(
    tab: TintoTab,
    selected: TintoTab,
    onTabSelected: (TintoTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinto = LocalTintoColors.current
    val color = if (tab == selected) tinto.gold else tinto.muted
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onTabSelected(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.labelRes),
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = LocalTintoTypography.current.meta,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun FabWithRing(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(FabRingSize)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(FabRingSize - 8.dp)
                .clip(FabShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.nav_add),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TintoBottomBarPreview() {
    TintoTheme {
        TintoBottomBar(selected = TintoTab.HOME, onTabSelected = {}, onAddClick = {})
    }
}
