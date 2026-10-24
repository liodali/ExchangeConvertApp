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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import dali.hamza.shared.ui.components.LedgerChartStyle
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerDestination
import dali.hamza.shared.ui.components.LedgerNavRail
import dali.hamza.shared.ui.components.LedgerRailDestination
import dali.hamza.shared.ui.components.LedgerWindowMode
import dali.hamza.shared.ui.components.isExpanded
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
import dali.hamza.shared.ui.screens.WidgetsScreen
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
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.legal_privacy
import echangecurrencyapp.shared.generated.resources.legal_terms
import echangecurrencyapp.shared.generated.resources.lock_locked
import echangecurrencyapp.shared.generated.resources.lock_not_setup
import echangecurrencyapp.shared.generated.resources.lock_try_again
import echangecurrencyapp.shared.generated.resources.lock_unlock
import echangecurrencyapp.shared.generated.resources.lock_verify
import org.jetbrains.compose.resources.stringResource
import dali.hamza.shared.ui.theme.LocalLedgerAppearance
import dali.hamza.shared.ui.viewmodel.AccountViewModel
import dali.hamza.shared.ui.viewmodel.HistoryViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.mp.KoinPlatform

/**
 * Route names of the Sovereign Ledger app (Navigation-Compose MP).
 *
 * Compact top-level (bottom bar visible): [HOME], [HISTORY], [ACCOUNT].
 * [SUPPORT] is a pushed route on compact layouts and a rail destination on
 * wider layouts. Other detail routes are pushed.
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val ACCOUNT = "account"
    const val CONVERTER = "converter"
    const val FULL_CONVERTER = "full-converter"
    const val RATE_ALERTS = "rate-alerts"
    const val WIDGETS = "widgets"
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

/** Maps any route onto the rail's five destinations (tablet shell). */private fun railForRoute(route: String?): LedgerRailDestination = when (route) {
    Routes.CONVERTER, Routes.FULL_CONVERTER -> LedgerRailDestination.RATES
    Routes.HISTORY -> LedgerRailDestination.HISTORY
    Routes.ACCOUNT -> LedgerRailDestination.ACCOUNT
    Routes.SUPPORT, Routes.FAQ, Routes.CONTACT, Routes.FEEDBACK, Routes.CHAT,
    Routes.TERMS, Routes.PRIVACY, Routes.ABOUT,
    -> LedgerRailDestination.SUPPORT
    else -> LedgerRailDestination.OVERVIEW
}

/** Two-letter avatar initials for the rail (design "HM"). */
private fun railInitials(username: String): String =
    username.split(' ', limit = 3)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "DN" }

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
    // chart rendering style (Account → Appearance): which graphs draw with
    var chartStyle by remember {
        mutableStateOf(LedgerChartStyle.fromStored(sessionStore?.getChartStyle()))
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
        // Keep compact navigation available on the support hub so users can
        // move directly to Account without backing out of Support first.
        val showCompactNavigation =
            currentRoute in Routes.topLevel || currentRoute == Routes.SUPPORT
        // one-shot → reactive: deep links work on cold start AND when the
        // app is already composed (iOS onOpenURL on a warm app)
        var historyPairOverride by remember { mutableStateOf<Pair<String, String>?>(null) }
        val deepLink = DeepLinkNav.pending
        LaunchedEffect(deepLink) {
            deepLink?.let { link ->
                when (link.route) {
                    "history" -> link.base?.let { b ->
                        link.quote?.let { q -> historyPairOverride = b to q }
                    }
                    // converter links prefill pair + amount, then convert
                    "full-converter", "converter" -> {
                        link.base?.let { code ->
                            dali.hamza.shared.data.CurrenciesCatalog.byCode(code)
                                ?.let(viewModel::selectFromCurrency)
                        }
                        link.quote?.let { code ->
                            dali.hamza.shared.data.CurrenciesCatalog.byCode(code)
                                ?.let(viewModel::selectToCurrency)
                        }
                        link.amount?.let { value ->
                            viewModel.onAmountChange(value)
                            viewModel.convert()
                        }
                    }
                }
                if (link.route != "home") {
                    runCatching { navController.navigate(link.route) }
                }
                DeepLinkNav.clear()
            }
        }
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

        // Canvas for BOTH branches: the onboarding branch has no app
        // container (Scaffold/Box below), so without this the raw light
        // window background bleeds through behind onboarding.
        Box(
            modifier = Modifier.fillMaxSize().background(LedgerColors.Canvas)
                // Protect the whole shell, including the rail, from landscape
                // cutouts and side navigation bars. Keep the canvas full bleed.
                .then(if (isIos()) Modifier else Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )),
        ) {
        if (!onboarded) {
            OnboardingScreen(
                currencies = sharedState.currencies,
                base = sharedState.marketBase.takeIf { it.isNotEmpty() },
                onDone = { baseCode, codes ->
                    // Persist AND refresh the AccountViewModel snapshot: it is
                    // created at first composition — before onboarding runs —
                    // so its cached marketPreferences would otherwise stay
                    // empty, and Account would show (and rebuild from) the
                    // stale list instead of this selection.
                    accountViewModel.replaceMarketPreferences(codes)
                    marketPreferences = codes
                    // apply the chosen base: persists it and refetches all
                    // rates quoted against the new base
                    viewModel.selectMarketBase(baseCode)
                    onboarded = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    // the theme canvas is painted by the app container, which
                    // is NOT composed while onboarding shows — without this
                    // the raw (light) window background bleeds through
                    .background(LedgerColors.Canvas)
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
        val appNavHost: @Composable (Modifier, LedgerWindowMode) -> Unit = { navModifier, mode ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = navModifier,
                // NATIVE iOS-style slide transitions:
                // push  → new page slides in from the right edge (full width)
                // back  → disposed page slides out to the right edge and FADES
                //         while sliding; the previous page slides in from the
                //         left third beneath it.
                // Each destination paints its own canvas backdrop
                // (see PageCanvas below), so a page always carries its
                // background with it — the disposed page's background can
                // never drop out mid-slide (the original iOS bug).
                enterTransition = {
                    slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))
                },
                exitTransition = {
                    slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350))
                },
            ) {
                composable(Routes.HOME) {
                    PageCanvas {
                    HomeScreen(
                        viewModel = viewModel,
                        marketPreferences = marketPreferences,
                        chartStyle = chartStyle,
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
                }
                composable(Routes.HISTORY) {
                    PageCanvas {
                    HistoryScreen(
                        sharedViewModel = viewModel,
                        historyViewModel = historyViewModel,
                        chartStyle = chartStyle,
                        pairOverride = historyPairOverride,
                        isExpanded = mode.isExpanded,
                    )
                    }
                }
                composable(Routes.ACCOUNT) {
                    PageCanvas {
                    AccountScreen(
                        viewModel = accountViewModel,
                        showSupportEntry = mode == LedgerWindowMode.COMPACT,
                        currencies = sharedState.currencies,
                        baseCurrency = sharedState.marketBase.takeIf { it.isNotEmpty() },
                        onMarketPreferencesChanged = { marketPreferences = it },
                        onBaseCurrencyChanged = viewModel::selectMarketBase,
                        onUsernameChanged = viewModel::onUsernameChanged,
                        onThemeModeChanged = { themeMode = it },
                        onColorPaletteChanged = { colorPalette = it },
                        onChartStyleChanged = { chartStyle = it },
                        onOpenSupport = {
                            navController.navigate(Routes.SUPPORT) {
                                if (mode != LedgerWindowMode.COMPACT) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenRateAlerts = { navController.navigate(Routes.RATE_ALERTS) },
                        onOpenWidgets = { navController.navigate(Routes.WIDGETS) },
                    )
                    }
                }
                composable(Routes.CONVERTER) {
                    PageCanvas {
                    ConverterCurrencyScreen(
                        viewModel = viewModel,
                        // separate instance from the History tab (Koin factory)
                        historyViewModel = remember {
                            KoinPlatform.getKoin()?.get<HistoryViewModel>() ?: historyViewModel
                        },
                        chartStyle = chartStyle,
                        onBack = { navController.popBackStack() },
                        onOpenFullConverter = { navController.navigate(Routes.FULL_CONVERTER) },
                        isExpanded = mode.isExpanded,
                    )
                    }
                }
                composable(Routes.FULL_CONVERTER) {
                    PageCanvas {
                    FullConverterScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                    }
                }
                composable(Routes.RATE_ALERTS) {
                    PageCanvas {
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
                }
                composable(Routes.WIDGETS) {
                    PageCanvas {
                    WidgetsScreen(
                        viewModel = remember {
                            dali.hamza.shared.ui.viewmodel.WidgetsViewModel(
                                repository = KoinPlatform.getKoin()?.get()
                                    ?: error("initSharedKoin() must run before ExchangeCurrencyApp()"),
                                storage = KoinPlatform.getKoin()?.get(),
                                // no platform bridge registered — use the
                                // platform factory (iOS reloads WidgetKit
                                // timelines; Android registers via Koin)
                                bridge = runCatching {
                                    KoinPlatform.getKoin()?.get<dali.hamza.shared.platform.WidgetsHostBridge>()
                                }.getOrNull()
                                    ?: dali.hamza.shared.platform.createWidgetsHostBridge(),
                            )
                        },
                        onBack = { navController.popBackStack() },
                    )
                    }
                }
                composable(Routes.FAQ) {
                    PageCanvas {
                    FaqScreen(
                        onBack = { navController.popBackStack() },
                        onOpenContact = { navController.navigate(Routes.CONTACT) },
                    )
                    }
                }
                composable(Routes.SUPPORT) {
                    PageCanvas {
                    SupportScreen(
                        onBack = if (mode == LedgerWindowMode.COMPACT) {
                            { navController.popBackStack() }
                        } else {
                            null
                        },
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
                }
                composable(Routes.TERMS) {
                    PageCanvas {
                    LegalScreen(
                        title = stringResource(Res.string.legal_terms),
                        url = dali.hamza.shared.platform.AppLinks.TERMS_URL,
                        onBack = { navController.popBackStack() },
                    )
                    }
                }
                composable(Routes.PRIVACY) {
                    PageCanvas {
                    LegalScreen(
                        title = stringResource(Res.string.legal_privacy),
                        url = dali.hamza.shared.platform.AppLinks.PRIVACY_URL,
                        onBack = { navController.popBackStack() },
                    )
                    }
                }
                composable(Routes.ABOUT) {
                    PageCanvas {
                    AboutScreen(
                        onOpenTerms = { navController.navigate(Routes.TERMS) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onBack = { navController.popBackStack() },
                    )
                    }
                }
                composable(Routes.CHAT) {
                    PageCanvas {
                    ChatwootScreen(
                        onBack = { navController.popBackStack() },
                        userName = sharedState.username,
                    )
                    }
                }
                composable(Routes.CONTACT) {
                    PageCanvas {
                    ContactSupportScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.FEEDBACK) {
                    PageCanvas {
                    FeedbackScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        // ---- adaptive shell (plans/tablet-ui.md) ---------------------------
        // COMPACT (phones, folded duos): bottom navigation, exactly as before.
        // MEDIUM/EXPANDED (tablets, iPad, unfolded): the design's 72dp rail.
        val navigateRail: (LedgerRailDestination) -> Unit = { destination ->
            when (destination) {
                LedgerRailDestination.OVERVIEW -> navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                LedgerRailDestination.RATES -> navController.navigate(Routes.CONVERTER) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                LedgerRailDestination.HISTORY -> {
                    // History tab = the app pair again (widget deep-link
                    // override only rides the pushed nav)
                    historyPairOverride = null
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                LedgerRailDestination.ACCOUNT -> navController.navigate(Routes.ACCOUNT) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                LedgerRailDestination.SUPPORT -> navController.navigate(Routes.SUPPORT) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        val navigateCompact: (LedgerDestination) -> Unit = { destination ->
            val returnsToAccount = destination == LedgerDestination.ACCOUNT &&
                currentRoute == Routes.SUPPORT &&
                navController.previousBackStackEntry?.destination?.route == Routes.ACCOUNT
            if (returnsToAccount) {
                navController.popBackStack()
            } else {
                if (destination == LedgerDestination.HISTORY) historyPairOverride = null
                navController.navigate(routeFor(destination)) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        // platform split, parameterized by window mode so both the bottom
        // bar path and the rail path share it
        val appShell: @Composable (LedgerWindowMode) -> Unit = { mode ->
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
                appNavHost(Modifier.fillMaxSize().then(keyboardSink), mode)

                // floating liquid-glass nav — on top of the scrolling content
                // (compact windows only; tablets use the design rail)
                if (mode == LedgerWindowMode.COMPACT && showCompactNavigation) {
                    LedgerBottomNav(
                        currentDestination = destinationForRoute(currentRoute),
                        onSelectDestination = navigateCompact,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        } else {
            Scaffold(
                // Headers own the top inset. Scaffold protects detail screens
                // without a bottom bar and uses the bar height when present.
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                modifier = modifier
                    .fillMaxSize()
                    .background(LedgerColors.Canvas)
                    .then(sinkModifier)
                    .blur(if (locked) 20.dp else 0.dp),
                bottomBar = {
                    if (mode == LedgerWindowMode.COMPACT && showCompactNavigation) {
                        LedgerBottomNav(
                            currentDestination = destinationForRoute(currentRoute),
                            onSelectDestination = navigateCompact,
                        )
                    }
                }
            ) { innerPadding ->
                appNavHost(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                        .then(keyboardSink),
                    mode,
                )
            }
        }
        }

        // ---- adaptive nav: bottom bar (compact) vs design rail (tablet) ----
        LedgerWindowMode { mode ->
            if (mode == LedgerWindowMode.COMPACT) {
                appShell(mode)
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    LedgerNavRail(
                        selected = railForRoute(currentRoute),
                        onSelect = navigateRail,
                        initials = railInitials(sharedState.username),
                        onSettings = {
                            navController.navigate(Routes.ACCOUNT) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        appShell(mode)
                    }
                }
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
    Routes.ACCOUNT, Routes.SUPPORT -> LedgerDestination.ACCOUNT
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
                text = "Dinarly",
                style = MaterialTheme.typography.headlineMedium,
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (cancelled) {
                    stringResource(Res.string.lock_verify)
                } else {
                    stringResource(Res.string.lock_locked)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (cancelled) LedgerColors.Gold else LedgerColors.TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            LedgerButton(
                text = when {
                    cancelled -> stringResource(Res.string.lock_try_again)
                    canPrompt -> stringResource(Res.string.lock_unlock)
                    else -> stringResource(Res.string.lock_not_setup)
                },
                onClick = onPrompt,
            )
        }
    }
}

/**
 * Opaque page backdrop for every NavHost destination: each page paints its
 * own canvas so a sliding page always CARRIES its background with it.
 * The app container's canvas stays fixed behind the NavHost and cannot
 * follow a page mid-transition — without this, the disposed page's
 * background visibly dropped out during the native slide (the iOS bug).
 */
@Composable
private fun PageCanvas(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(LedgerColors.Canvas)) {
        content()
    }
}
