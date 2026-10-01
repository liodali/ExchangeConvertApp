package dali.hamza.shared.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.ui.components.LedgerBottomNav
import dali.hamza.shared.ui.components.LedgerDestination
import dali.hamza.shared.ui.screens.AccountScreen
import dali.hamza.shared.ui.screens.ContactSupportScreen
import dali.hamza.shared.ui.screens.FaqScreen
import dali.hamza.shared.ui.screens.FeedbackScreen
import dali.hamza.shared.ui.screens.FullConverterScreen
import dali.hamza.shared.ui.screens.HistoryScreen
import dali.hamza.shared.ui.screens.SupportScreen
import dali.hamza.shared.ui.screens.ConverterCurrencyScreen
import dali.hamza.shared.ui.screens.HomeScreen
import dali.hamza.shared.ui.viewmodel.HomeViewModel
import dali.hamza.shared.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.shared.ui.viewmodel.AccountViewModel
import dali.hamza.shared.ui.viewmodel.HistoryViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.mp.KoinPlatform

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
    const val FULL_CONVERTER = "full-converter"
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
        val keyboardController = LocalSoftwareKeyboardController.current
        // Focus sink: an invisible, non-input focus target. Tapping anywhere
        // moves focus HERE instead of "clearing" it — Compose's root focus
        // restoration would otherwise re-focus the amount field and reopen
        // the keyboard.
        val focusSink = remember { FocusRequester() }
        var sinkHasFocus by remember { mutableStateOf(false) }
        val sinkModifier = Modifier
            .focusRequester(focusSink)
            .onFocusChanged { sinkHasFocus = it.isFocused }
            .focusTarget()
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        // Hosts always start Koin before composing — resolve the cluster VMs
        // from the shared container (same instance both platforms use).
        val accountViewModel = remember {
            KoinPlatform.getKoin()?.get<AccountViewModel>()
                ?: AccountViewModel(dali.hamza.shared.data.storage.createSessionStorage())
        }
        val historyViewModel = remember {
            HistoryViewModel(
                KoinPlatform.getKoin()?.get<IRepository>()
                    ?: error("initSharedKoin() must run before ExchangeCurrencyApp()")
            )
        }

        Scaffold(
            // full-bleed canvas — hosts opt into system-bar insets where the
            // design needs them. ANY touch clears focus first (observing,
            // non-consuming): taps on empty space dismiss the keyboard; taps
            // on another input re-focus it and the keyboard stays.
            contentWindowInsets = WindowInsets(0.dp),
            modifier = modifier
                .fillMaxSize()
                .then(sinkModifier),
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
                // Keyboard UX: on touch-DOWN focus moves to the invisible
                // sink (never clearFocus — Compose's focus restoration would
                // re-focus the amount field). After the gesture ENDS, the IME
                // is hidden as the final word — but only if no input re-took
                // the focus (tapping the field itself keeps the keyboard).
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            runCatching { focusSink.requestFocus() }
                            waitForUpOrCancellation()
                            if (sinkHasFocus) {
                                keyboardController?.hide()
                            }
                        }
                    },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        homeViewModel = remember {
                            KoinPlatform.getKoin()?.get<HomeViewModel>()
                                ?: HomeViewModel(
                                    KoinPlatform.getKoin()?.get<IRepository>()
                                        ?: error("initSharedKoin() must run first")
                                )
                        },
                        onOpenConverter = { navController.navigate(Routes.CONVERTER) },
                        onOpenHistory = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        sharedViewModel = viewModel,
                        historyViewModel = historyViewModel,
                    )
                }
                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        viewModel = accountViewModel,
                        onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    )
                }
                composable(Routes.CONVERTER) {
                    ConverterCurrencyScreen(
                        viewModel = viewModel,
                        // separate instance from the History tab (Koin factory)
                        historyViewModel = remember {
                            KoinPlatform.getKoin()?.get<HistoryViewModel>() ?: historyViewModel
                        },
                        onBack = { navController.popBackStack() },
                        onOpenFullConverter = { navController.navigate(Routes.FULL_CONVERTER) },
                    )
                }
                composable(Routes.FULL_CONVERTER) {
                    FullConverterScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.FAQ) {
                    FaqScreen(
                        onBack = { navController.popBackStack() },
                        onOpenContact = { navController.navigate(Routes.CONTACT) },
                    )
                }
                composable(Routes.SUPPORT) {
                    SupportScreen(
                        onBack = { navController.popBackStack() },
                        onOpenFaq = { navController.navigate(Routes.FAQ) },
                        onOpenContact = { navController.navigate(Routes.CONTACT) },
                        onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                    )
                }
                composable(Routes.CONTACT) {
                    ContactSupportScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.FEEDBACK) {
                    FeedbackScreen(onBack = { navController.popBackStack() })
                }
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
