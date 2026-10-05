package dali.hamza.shared.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dali.hamza.shared.data.storage.createSessionStorage
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.BiometricAuthenticator
import dali.hamza.shared.platform.createBiometricAuthenticator
import dali.hamza.shared.platform.isIos
import dali.hamza.shared.ui.components.LedgerBottomNav
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerDestination
import dali.hamza.shared.ui.screens.AccountScreen
import dali.hamza.shared.ui.screens.AboutScreen
import dali.hamza.shared.ui.screens.ChatwootScreen
import dali.hamza.shared.ui.screens.LegalScreen
import dali.hamza.shared.ui.screens.ContactSupportScreen
import dali.hamza.shared.ui.screens.FaqScreen
import dali.hamza.shared.ui.screens.FeedbackScreen
import dali.hamza.shared.ui.screens.FullConverterScreen
import dali.hamza.shared.ui.screens.HistoryScreen
import dali.hamza.shared.ui.screens.RateAlertsScreen
import dali.hamza.shared.ui.screens.SupportScreen
import dali.hamza.shared.ui.screens.ConverterCurrencyScreen
import dali.hamza.shared.ui.screens.HomeScreen
import dali.hamza.shared.ui.screens.OnboardingScreen
import dali.hamza.shared.ui.viewmodel.HomeViewModel
import dali.hamza.shared.ui.viewmodel.RateAlertsViewModel
import dali.hamza.shared.ui.theme.ExchangeCurrencyAppTheme
import dali.hamza.shared.ui.theme.LedgerAppearance
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerThemeMode
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LocalLedgerAppearance
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
    const val RATE_ALERTS = "rate-alerts"
    const val FAQ = "faq"
    const val SUPPORT = "support"
    const val CONTACT = "contact"
    const val FEEDBACK = "feedback"
    const val CHAT = "chat"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

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
    // appearance (Account → Appearance): storage-backed so the choice
    // survives restarts; SYSTEM follows the platform setting
    val sessionStore = remember { runCatching { createSessionStorage() }.getOrNull() }
    var themeMode by remember {
        mutableStateOf(LedgerThemeMode.fromStored(sessionStore?.getThemeMode()))
    }
    var colorPalette by remember {
        mutableStateOf(LedgerColorPalette.fromStored(sessionStore?.getColorPalette()))
    }
    ExchangeCurrencyAppTheme(themeMode = themeMode, colorPalette = colorPalette) {
        val keyboardController = LocalSoftwareKeyboardController.current
        // system-bar contrast follows the resolved theme (light mode → dark icons)
        val appearance = LocalLedgerAppearance.current
        LaunchedEffect(appearance.dark) {
            dali.hamza.shared.platform.applySystemBarIcons(appearance.dark)
        }
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

        // ---- biometric app lock (guest & sovereign; off by default) --------
        val authenticator = remember { dali.hamza.shared.platform.createBiometricAuthenticator() }
        var lockEnabled by remember { mutableStateOf(sessionStore?.getBiometricUnlock() == true) }
        var unlocked by remember { mutableStateOf(false) }
        var authCancelled by remember { mutableStateOf(false) }
        val locked = lockEnabled && !unlocked

        // ---- market-overview onboarding (first launch) ---------------------
        // empty preferences = the user never completed the market selection
        var marketPreferences by remember {
            mutableStateOf(sessionStore?.getMarketPreferences().orEmpty())
        }
        var onboarded by remember { mutableStateOf(marketPreferences.isNotEmpty()) }
        val sharedState by viewModel.state.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
        if (!onboarded) {
            OnboardingScreen(
                currencies = sharedState.currencies,
                base = sharedState.fromCurrency?.name,
                onDone = { baseCode, codes ->
                    sessionStore?.setMarketPreferences(codes)
                    marketPreferences = codes
                    // apply the chosen base: persists it and refetches all
                    // rates quoted against the new base
                    if (baseCode != sharedState.fromCurrency?.name) {
                        sharedState.currencies
                            .firstOrNull { it.name == baseCode }
                            ?.let(viewModel::selectFromCurrency)
                    }
                    onboarded = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (locked) 20.dp else 0.dp),
            )
        } else {
        // Platform split: iOS hosts the floating liquid-glass capsule over
        // full-bleed content (screens add ledgerNavClearance()). Android
        // keeps the classic Scaffold bottomBar slot — content stops at the
        // bar's top edge, exactly as before the glass redesign.
        val keyboardSink = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                runCatching { focusSink.requestFocus() }
                waitForUpOrCancellation()
                if (sinkHasFocus) {
                    keyboardController?.hide()
                }
            }
        }
        val appNavHost: @Composable (Modifier) -> Unit = { navModifier ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = navModifier,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        marketPreferences = marketPreferences,
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
                        onOpenRateAlerts = { navController.navigate(Routes.RATE_ALERTS) },
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
                        currencies = sharedState.currencies,
                        baseCurrency = sharedState.fromCurrency?.name,
                        onMarketPreferencesChanged = { marketPreferences = it },
                        onBaseCurrencyChanged = { code ->
                            sharedState.currencies
                                .firstOrNull { it.name == code }
                                ?.let(viewModel::selectFromCurrency)
                        },
                        onThemeModeChanged = { themeMode = it },
                        onColorPaletteChanged = { colorPalette = it },
                        onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                        onOpenRateAlerts = { navController.navigate(Routes.RATE_ALERTS) },
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
                composable(Routes.RATE_ALERTS) {
                    RateAlertsScreen(
                        viewModel = remember {
                            KoinPlatform.getKoin()?.get<RateAlertsViewModel>()
                                ?: error("initSharedKoin() must run before ExchangeCurrencyApp()")
                        },
                        currencies = sharedState.currencies,
                        defaultBase = sharedState.fromCurrency?.name,
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
                        // feedback lives in the Chatwoot inbox — the form
                        // screen stays reachable via CONTACT's flow if needed
                        onOpenFeedback = { navController.navigate(Routes.CHAT) },
                        onOpenTerms = { navController.navigate(Routes.TERMS) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    )
                }
                composable(Routes.TERMS) {
                    LegalScreen(
                        title = "Terms & Conditions",
                        url = dali.hamza.shared.platform.AppLinks.TERMS_URL,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.PRIVACY) {
                    LegalScreen(
                        title = "Privacy Policy",
                        url = dali.hamza.shared.platform.AppLinks.PRIVACY_URL,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen(
                        onOpenTerms = { navController.navigate(Routes.TERMS) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.CHAT) {
                    ChatwootScreen(
                        onBack = { navController.popBackStack() },
                        userName = sharedState.username,
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

        if (isIos()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    // Canvas = the app background (what the removed
                    // Scaffold's containerColor used to paint)
                    .background(LedgerColors.Canvas)
                    .then(sinkModifier)
                    .blur(if (locked) 20.dp else 0.dp),
            ) {
                appNavHost(Modifier.fillMaxSize().then(keyboardSink))

                // floating liquid-glass nav — on top of the scrolling content
                if (currentRoute in Routes.topLevel) {
                    LedgerBottomNav(
                        currentDestination = destinationForRoute(currentRoute),
                        onSelectDestination = { destination ->
                            navController.navigate(routeFor(destination)) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        } else {
            Scaffold(
                // full-bleed canvas — hosts opt into system-bar insets where
                // the design needs them (original Android behaviour).
                contentWindowInsets = WindowInsets(0.dp),
                modifier = modifier
                    .fillMaxSize()
                    .background(LedgerColors.Canvas)
                    .then(sinkModifier)
                    .blur(if (locked) 20.dp else 0.dp),
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
                appNavHost(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(keyboardSink)
                )
            }
        }
        }

            if (locked) {
                LockScreen(
                    canPrompt = authenticator.isAvailable(),
                    cancelled = authCancelled,
                    onPrompt = {
                        authenticator.authenticate { ok ->
                            if (ok) {
                                unlocked = true
                                authCancelled = false
                            } else {
                                authCancelled = true
                            }
                        }
                    },
                )
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



/**
 * Biometric gate — the app renders behind (Home/splash) but is blurred
 * and scrimmed until the user authenticates. Cancelling the system prompt
 * shows a must-authenticate message; only a successful check unlocks.
 */
@Composable
private fun LockScreen(
    canPrompt: Boolean,
    cancelled: Boolean,
    onPrompt: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (canPrompt) onPrompt()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerColors.Scrim.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LedgerLogoMark(size = 72.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Sovereign Ledger",
                style = MaterialTheme.typography.headlineMedium,
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (cancelled) {
                    "You need to verify your biometrics to continue"
                } else {
                    "Your ledger is locked"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (cancelled) LedgerColors.Gold else LedgerColors.TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            LedgerButton(
                text = when {
                    cancelled -> "Try again"
                    canPrompt -> "Unlock"
                    else -> "Biometric not set up — tap to retry"
                },
                onClick = onPrompt,
            )
        }
    }
}
