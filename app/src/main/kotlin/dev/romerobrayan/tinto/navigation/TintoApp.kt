package dev.romerobrayan.tinto.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.romerobrayan.tinto.core.designsystem.component.TintoScaffold
import dev.romerobrayan.tinto.core.designsystem.component.TintoTab
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionScreen
import dev.romerobrayan.tinto.feature.dashboard.DashboardScreen
import dev.romerobrayan.tinto.feature.movements.MovementsScreen
import dev.romerobrayan.tinto.feature.profile.ProfileScreen
import dev.romerobrayan.tinto.feature.reminders.RemindersScreen

/**
 * App frame: bottom bar + center FAB around the NavHost. The bar hides on
 * the add-transaction screen (capture mode is full-screen).
 *
 * @param onScreenView reports destination changes to analytics as `screen_view`.
 */
@Composable
fun TintoApp(onScreenView: (String) -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab = when {
        currentDestination == null -> TintoTab.HOME
        currentDestination.hasRoute<DashboardRoute>() -> TintoTab.HOME
        currentDestination.hasRoute<MovementsRoute>() -> TintoTab.MOVEMENTS
        currentDestination.hasRoute<RemindersRoute>() -> TintoTab.REMINDERS
        currentDestination.hasRoute<ProfileRoute>() -> TintoTab.PROFILE
        else -> null
    }

    val screenName = when {
        currentDestination == null -> null
        currentDestination.hasRoute<DashboardRoute>() -> "dashboard"
        currentDestination.hasRoute<MovementsRoute>() -> "movements"
        currentDestination.hasRoute<AddTransactionRoute>() -> "add_transaction"
        currentDestination.hasRoute<RemindersRoute>() -> "reminders"
        currentDestination.hasRoute<ProfileRoute>() -> "profile"
        else -> null
    }
    LaunchedEffect(screenName) {
        screenName?.let(onScreenView)
    }

    val onEditMovement: (String) -> Unit = { transactionId ->
        navController.navigate(AddTransactionRoute(transactionId))
    }

    TintoScaffold(
        currentTab = currentTab,
        onTabSelected = navController::navigateToTab,
        onAddClick = { navController.navigate(AddTransactionRoute()) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onSeeAll = { navController.navigateToTab(TintoTab.MOVEMENTS) },
                    onEditMovement = onEditMovement,
                )
            }
            composable<MovementsRoute> {
                MovementsScreen(onEditMovement = onEditMovement)
            }
            composable<AddTransactionRoute> {
                AddTransactionScreen(onClose = { navController.popBackStack() })
            }
            composable<RemindersRoute> {
                RemindersScreen()
            }
            composable<ProfileRoute> {
                ProfileScreen()
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: TintoTab) {
    val route: Any = when (tab) {
        TintoTab.HOME -> DashboardRoute
        TintoTab.MOVEMENTS -> MovementsRoute
        TintoTab.REMINDERS -> RemindersRoute
        TintoTab.PROFILE -> ProfileRoute
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
