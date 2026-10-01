package dali.hamza.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dali.hamza.shared.ui.components.LedgerBottomNav
import dali.hamza.shared.ui.components.LedgerDestination
import dali.hamza.shared.screens.AccountPlaceholder
import dali.hamza.shared.screens.ContactPlaceholder
import dali.hamza.shared.screens.FaqPlaceholder
import dali.hamza.shared.screens.FeedbackPlaceholder
import dali.hamza.shared.screens.SupportPlaceholder
import dali.hamza.shared.ui.screens.ConverterCurrencyScreen
import dali.hamza.shared.ui.screens.HomeScreen
import dali.hamza.shared.ui.screens.RatesScreen
import dali.hamza.shared.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Route names of the Sovereign Ledger app (Navigation-Compose MP).
 *
 * Top-level (bottom bar visible): [HOME], [HISTORY], [ACCOUNT].
 * Pushed (bottom bar hidden): [CONVERTER], [FAQ], [SUPPORT], [CONTACT], [FEEDBACK].
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val ACCOUNT = "account"
    const val CONVERTER = "converter"
    const val FAQ = "faq"
    const val SUPPORT = "support"
    const val CONTACT = "contact"
    const val FEEDBACK = "feedback"

    val topLevel = setOf(HOME, HISTORY, ACCOUNT)
}

/**
 * Root of the shared Compose Multiplatform app.
 * Hosted by:
 * - Android: MainActivity (planned, see plans/kmp-ui-unification-plan.md Phase 3/4)
 * - iOS:     MainViewController() → SwiftUI wrapper
 */
@Composable
fun ExchangeCurrencyApp(
    viewModel: SharedViewModel,
    modifier: Modifier = Modifier,
) {
    ExchangeCurrencyAppTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            // full-bleed canvas — hosts opt into system-bar insets where the
            // design needs them (Android runs immersive fullscreen)
            modifier = modifier
                .fillMaxSize(),
            bottomBar = {
                if (currentRoute in Routes.topLevel) {
                    LedgerBottomNav(
                        currentDestination = destinationForRoute(currentRoute),
                        onSelectDestination = { destination ->
                            navController.navigate(routeFor(destination)) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenConverter = { navController.navigate(Routes.CONVERTER) }
                    )
                }
                composable(Routes.HISTORY) {
                    // Placeholder until Phase 4 (History cluster) — shows the
                    // current rates list so the tab stays useful.
                    RatesScreen(viewModel)
                }
                composable(Routes.ACCOUNT) { AccountPlaceholder() }
                composable(Routes.CONVERTER) { ConverterCurrencyScreen(viewModel) }
                composable(Routes.FAQ) { FaqPlaceholder() }
                composable(Routes.SUPPORT) {
                    SupportPlaceholder(
                        onOpenContact = { navController.navigate(Routes.CONTACT) },
                        onOpenFeedback = { navController.navigate(Routes.FEEDBACK) }
                    )
                }
                composable(Routes.CONTACT) { ContactPlaceholder() }
                composable(Routes.FEEDBACK) { FeedbackPlaceholder() }
            }
        }
    }
}

private fun routeFor(destination: LedgerDestination): String = when (destination) {
    LedgerDestination.HOME -> Routes.HOME
    LedgerDestination.HISTORY -> Routes.HISTORY
    LedgerDestination.ACCOUNT -> Routes.ACCOUNT
}

private fun destinationForRoute(route: String?): LedgerDestination = when (route) {
    Routes.HISTORY -> LedgerDestination.HISTORY
    Routes.ACCOUNT -> LedgerDestination.ACCOUNT
    else -> LedgerDestination.HOME
}
