package dali.hamza.shared.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.platform.currentHourOfDay
import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.ui.components.AddRateAlertDialog
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerChartStyle
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.Sparkline
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.components.ledgerNavClearance
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.theme.LedgerStrings
import dali.hamza.shared.ui.viewmodel.HomeViewModel
import dali.hamza.shared.ui.viewmodel.PairCardData
import dali.hamza.shared.ui.viewmodel.RateAlertsViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.mp.KoinPlatform

/**
 * Sovereign Market Dashboard (design frame `RbQhR`) — Home tab.
 *
 * Layout follows the pen file section by section:
 * 1. Market Overview — top pairs with live rates (mini charts land with
 *    Phase 4 /historical data)
 * 2. Asset Explorer & Conversion Tool — Quick Exchange with fee row
 *    (design copy) and "Execute Exchange" action
 * 3. Recent Activity — minimized bento; transactions arrive in Phase 4
 *
 * The top bar shows the profile username (design: "SOVEREIGN"), not the
 * app name.
 */
@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    homeViewModel: HomeViewModel,
    onOpenConverter: () -> Unit,
    onOpenHistory: () -> Unit,
    marketPreferences: List<String> = emptyList(),
    onOpenRateAlerts: () -> Unit = {},
    chartStyle: LedgerChartStyle = LedgerChartStyle.LINE,
) {
    val state by viewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    var pickerFor by remember { mutableStateOf<Boolean?>(null) } // true=from, false=to, null=hidden

    // rate alerts: bell on the market cards — a tracked market becomes a
    // local alert in one tap (add dialog pre-filled with the pair)
    val rateAlertsViewModel = remember {
        KoinPlatform.getKoin()?.get<RateAlertsViewModel>()
            ?: error("initSharedKoin() must run before HomeScreen()")
    }
    var alertQuote by remember { mutableStateOf<String?>(null) }

    val topRates = state.rates.topPairs()

    // market-overview cards: the user's onboarding selections (Account can
    // change them); falls back to the design majors while nothing is chosen
    val cardsRates = remember(state.rates, marketPreferences, state.fromCurrency?.name) {
        val base = state.fromCurrency?.name
        marketPreferences
            .filter { it != base }
            .mapNotNull { code -> state.rates.firstOrNull { it.name == code } }
            .ifEmpty { topRates.take(3) }
    }

    // pair cards: reload when the base or selected rates change
    LaunchedEffect(state.fromCurrency?.name, cardsRates) {
        homeViewModel.load(state.fromCurrency?.name, cardsRates)
    }
    // recent activity: keep fresh while Home is visible
    LaunchedEffect(Unit) {
        homeViewModel.loadTransactions()
        while (true) {
            kotlinx.coroutines.delay(10_000)
            homeViewModel.refreshTransactions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = state.username,
            leading = { LedgerLogoMark() },
            greeting = remember { greetingForHour(currentHourOfDay()) },
            avatarInitials = state.username,
            trailing = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        Icons.Outlined.Sync,
                        contentDescription = "Refresh rates",
                        tint = LedgerColors.Blue,
                    )
                }
            },
        )

        // ---- design rhythm: 24dp side margins, 32dp between sections -----
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            Spacer(Modifier.height(24.dp))

            // ============ 1. Market Overview ================================
            MarketOverviewSection(
                pairCards = homeState.pairCards.ifEmpty {
                    cardsRates.map { PairCardData(quote = it.name, rate = it.rate) }
                },
                base = state.fromCurrency?.name,
                alertedQuotes = rateAlertsViewModel.alerts.map { it.quote }.toSet(),
                onAddAlert = { alertQuote = it },
                onOpenRateAlerts = onOpenRateAlerts,
                chartStyle = chartStyle,
            )

            Spacer(Modifier.height(32.dp))

            // ============ 2. Quick Exchange =================================
            QuickExchangeSection(
                amount = state.amount,
                onAmountChange = viewModel::onAmountChange,
                from = state.fromCurrency,
                to = state.toCurrency,
                convertedAmount = state.convertedAmount,
                isLoading = state.isLoading,
                error = state.error,
                onPickFrom = { pickerFor = true },
                onPickTo = { pickerFor = false },
                onSwap = viewModel::swapCurrencies,
                onExecute = viewModel::convert,
                onOpenConverter = onOpenConverter,
            )

            Spacer(Modifier.height(32.dp))

            // ============ 3. Recent Activity (minimized) ====================
            RecentActivitySection(
                transactions = homeState.recentTransactions,
                onOpenHistory = onOpenHistory,
            )

            Spacer(Modifier.height(48.dp))
        }
    }

    pickerFor?.let { pickingFrom ->
        CurrencyPickerSheet(
            visible = true,
            currencies = state.currencies,
            title = if (pickingFrom) "From currency" else "To currency",
            onDismiss = { pickerFor = null },
            onCurrencySelected = { currency ->
                if (pickingFrom) viewModel.selectFromCurrency(currency)
                else viewModel.selectToCurrency(currency)
                pickerFor = null
            },
        )
    }

    // market-card bell → add an alert for that pair
    alertQuote?.let { quote ->
        AddRateAlertDialog(
            currencies = state.currencies,
            initialBase = state.fromCurrency?.name,
            initialQuote = quote,
            atCap = rateAlertsViewModel.alerts.size >= rateAlertsViewModel.maxAlerts,
            onDismiss = { alertQuote = null },
            onConfirm = { base, quoteCurrency, mode, intervalMinutes, thresholdPercent ->
                alertQuote = null
                rateAlertsViewModel.addAlert(
                    base,
                    quoteCurrency,
                    mode,
                    intervalMinutes,
                    thresholdPercent,
                )
            },
        )
    }
}

// ------------------------------------------------------ 1. market overview

/** Top pairs — majors first, MAD included (design shows EUR/BTC/XAU rows). */
private val TOP_PAIRS = listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD")

private fun List<ExchangeRate>.topPairs(): List<ExchangeRate> =
    TOP_PAIRS.mapNotNull { symbol -> firstOrNull { it.name == symbol } }

@Composable
private fun MarketOverviewSection(
    pairCards: List<PairCardData>,
    base: String?,
    alertedQuotes: Set<String>,
    onAddAlert: (String) -> Unit,
    onOpenRateAlerts: () -> Unit,
    chartStyle: LedgerChartStyle = LedgerChartStyle.LINE,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SectionHeader(
            title = "Market Overview",
            icon = Icons.Outlined.CurrencyExchange,
            modifier = Modifier.weight(1f),
        )
        LiveChip()
    }
    Spacer(Modifier.height(16.dp))

    if (pairCards.isEmpty()) {
        BentoCard(fill = LedgerColors.Card) {
            Text(
                text = "Loading live rates…",
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextTertiary,
            )
        }
        return
    }

    // long-press drag reordering lives in Onboarding + Account → Market
    // Preferences (same persisted order); Home cards just render it
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pairCards.forEach { card ->
            val hasAlert = card.quote in alertedQuotes
            PairCard(
                card = card,
                base = base,
                hasAlert = hasAlert,
                onAlert = {
                    // tracked market → manage it; untracked → offer to add
                    if (hasAlert) onOpenRateAlerts() else onAddAlert(card.quote)
                },
                chartStyle = chartStyle,
            )
        }
    }
}

/**
 * Design "EUR/USD Card" (updated pen): full-width #2A2A2A card —
 * one-line pair title + full name, 7-day delta chip, 4-decimal rate
 * (Manrope 700/30) and the 64dp warm-white sparkline. The trailing bell
 * adds (or manages) the local rate alert for the pair.
 */
@Composable
private fun PairCard(
    card: PairCardData,
    base: String?,
    hasAlert: Boolean,
    onAlert: () -> Unit,
    chartStyle: LedgerChartStyle = LedgerChartStyle.LINE,
) {
    val pairTitle = base?.let { "${card.quote}/$it" } ?: card.quote
    BentoCard(
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // full pair title (design "EUR/USD Card"): quote against
                    // the session base, so the card answers "compared to what?"
                    text = pairTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = LedgerColors.TextPrimary,
                )
                Text(
                    text = shortCurrencyName(card.quote),
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }
            card.deltaPercent?.let { delta ->
                DeltaChip(percent = delta)
            }
            Spacer(Modifier.width(12.dp))
            AlertBell(pair = pairTitle, hasAlert = hasAlert, onAlert = onAlert)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = formatRateDigits(card.rate, 4),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            ),
            color = LedgerColors.TextPrimary,
        )
        if (card.spark.size >= 2) {
            Spacer(Modifier.height(16.dp))
            Sparkline(
                values = card.spark,
                color = LedgerColors.TextPrimary,
                style = chartStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

/**
 * Market-card bell — muted/outline when the pair has no alert (tap: add),
 * gold/active when one exists (tap: manage alerts).
 */
@Composable
private fun AlertBell(
    pair: String,
    hasAlert: Boolean,
    onAlert: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (hasAlert) LedgerColors.Gold.copy(alpha = 0.12f) else LedgerColors.NavyDeep
            )
            .clickable(onClick = onAlert),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (hasAlert) {
                Icons.Outlined.NotificationsActive
            } else {
                Icons.Outlined.NotificationsNone
            },
            contentDescription = if (hasAlert) {
                LedgerStrings.RateAlerts.alertActiveFor(pair)
            } else {
                LedgerStrings.RateAlerts.addAlertFor(pair)
            },
            tint = if (hasAlert) LedgerColors.Gold else LedgerColors.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 7-day change chip: tiny direction arrow + Inter 600/14, green/red. */
@Composable
private fun DeltaChip(percent: Double) {
    val positive = percent >= 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                (if (positive) LedgerColors.Green else LedgerColors.Error).copy(alpha = 0.10f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = if (positive) {
                Icons.Outlined.ArrowDropUp
            } else {
                Icons.Outlined.ArrowDropDown
            },
            contentDescription = null,
            tint = if (positive) LedgerColors.Green else LedgerColors.Error,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = (if (positive) "+" else "-") +
                formatLedgerNumber(kotlin.math.abs(percent)).trimEnd('0').trimEnd('.') + "%",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (positive) LedgerColors.Green else LedgerColors.Error,
        )
    }
}

/** Design "LIVE" badge (Inter 600/10, green). */
@Composable
private fun LiveChip() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live-alpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LedgerColors.GreenGlow)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(LedgerColors.Green)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.Green,
        )
    }
}

// ------------------------------------------------------- 2. quick exchange

@Composable
private fun QuickExchangeSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    from: Currency?,
    to: Currency?,
    convertedAmount: Double?,
    isLoading: Boolean,
    error: String?,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSwap: () -> Unit,
    onExecute: () -> Unit,
    onOpenConverter: () -> Unit,
) {
    SectionHeader(title = "Quick Exchange", icon = Icons.Outlined.SwapHoriz)
    Spacer(Modifier.height(16.dp))

    BentoCard(fill = LedgerColors.NavyPanel) {
        LedgerInput(
            value = amount,
            onValueChange = onAmountChange,
            label = "Amount",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(16.dp))

        CurrencyRow(label = "From", currency = from, onClick = onPickFrom)

        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(start = 16.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LedgerColors.GreenSoft)
                .clickable(onClick = onSwap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.SwapHoriz,
                contentDescription = "Swap currencies",
                tint = LedgerColors.Green,
            )
        }

        CurrencyRow(label = "To", currency = to, onClick = onPickTo)

        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.Error,
            )
            Spacer(Modifier.height(12.dp))
        }

        val result = convertedAmount
        Text(
            text = when {
                result != null -> formatLedgerNumber(result)
                isLoading -> "…"
                else -> "—"
            },
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = if (result != null) LedgerColors.Green else LedgerColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        // design fee row: "Fee (0.01%)  $0.10 USD"
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Fee (0.01%)",
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextMuted,
            )
            Text(
                text = "\$0.10 USD",
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))

        LedgerButton(
            text = "Execute Exchange",
            onClick = onExecute,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )
        Spacer(Modifier.height(8.dp))
        LedgerButton(
            text = "Open full converter",
            onClick = onOpenConverter,
            modifier = Modifier.fillMaxWidth(),
            variant = LedgerButtonVariant.GHOST,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowRightAlt,
        )
    }
}

@Composable
private fun CurrencyRow(
    label: String,
    currency: Currency?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(LedgerColors.NavyDeep)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LedgerColors.TextMuted,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency?.name ?: "Select…",
                style = MaterialTheme.typography.titleMedium,
                color = LedgerColors.TextPrimary,
            )
            currency?.let {
                Text(
                    text = it.fullCountryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }
        }
        Text(
            text = "Change",
            style = MaterialTheme.typography.labelSmall,
            color = LedgerColors.BlueSoft,
        )
    }
}

// ----------------------------------------------------- 3. recent activity

@Composable
private fun RecentActivitySection(
    transactions: List<Transaction>,
    onOpenHistory: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            color = LedgerColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable(onClick = onOpenHistory),
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerColors.Blue,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
                contentDescription = "Open history",
                tint = LedgerColors.Blue,
                modifier = Modifier.size(12.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    BentoCard(fill = LedgerColors.SurfaceElevated) {
        if (transactions.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LedgerColors.Green.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = LedgerColors.Green,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "No exchanges yet — execute a conversion to start your ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerColors.TextSecondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                transactions.forEachIndexed { index, tx ->
                    ActivityRow(tx = tx, tint = if (index % 2 == 0) LedgerColors.Green else LedgerColors.Blue)
                }
            }
        }
    Spacer(Modifier.height(ledgerNavClearance()))    // liquid-glass nav clearance
    }
}

/** Design activity row: icon tile + "Exchanged X to Y" + time, +received on the right. */
@Composable
private fun ActivityRow(tx: Transaction, tint: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LedgerColors.Card.copy(alpha = 0.50f))
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CurrencyExchange,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Exchanged ${tx.base} to ${tx.quote}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = LedgerColors.TextPrimary,
            )
            Text(
                text = activityTimeLabel(tx.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = LedgerColors.TextTertiary,
            )
        }
        Text(
            text = "+" + formatLedgerNumber(tx.amountQuote) + " " + tx.quote,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = LedgerColors.Green,
        )
    }
}

/** "Today, 15:30" / "Yesterday, 08:15" / "Oct 12, 09:05" style labels. */
private fun activityTimeLabel(timestamp: Long): String {
    val now = dali.hamza.shared.common.nowMillis()
    val today = dali.hamza.shared.common.DateUtils.epochDayOf(now)
    val day = dali.hamza.shared.common.DateUtils.epochDayOf(timestamp)
    val time = DateUtils.formatDateTime(timestamp).substringAfter(' ')
    return when (today - day) {
        0L -> "Today, $time"
        1L -> "Yesterday, $time"
        else -> DateUtils.formatDateTime(timestamp).let {
            it.substring(5, 10).replace('-', '/') + ", " + time
        }
    }
}

// ------------------------------------------------------------------ helpers

/**
 * Time-based greeting shown above the username — used until a login /
 * guest profile exists (Account phase).
 */
internal fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Good Night"
}

/**
 * Ledger-style number formatting (KMP-safe, locale-stable):
 * 3 fraction digits everywhere (design decision).
 */
internal fun formatLedgerNumber(value: Double): String {
    val precision = 3
    val factor = pow10(precision)
    val rounded = kotlin.math.round(value * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}

/**
 * Short display names for pair cards — curated for majors, falls back to
 * the catalog's full name.
 */
internal fun shortCurrencyName(code: String): String = when (code) {
    "EUR" -> "Euro"
    "GBP" -> "Pound"
    "USD" -> "US Dollar"
    "MAD" -> "Dirham"
    "DZD" -> "Dinar"
    "TND" -> "Dinar"
    "JPY" -> "Yen"
    "CHF" -> "Franc"
    "CAD" -> "Cad Dollar"
    "AUD" -> "Aus Dollar"
    "NZD" -> "NZ Dollar"
    "CNY" -> "Yuan"
    "TRY" -> "Lira"
    "SAR" -> "Riyal"
    "AED" -> "Emir. Dirham"
    "EGP" -> "Egypt. Pound"
    "ZAR" -> "Rand"
    "INR" -> "Rupee"
    "SEK" -> "Krona"
    "NOK", "DKK" -> "Krone"
    "PLN" -> "Zloty"
    "CZK" -> "Koruna"
    "HKD" -> "HK Dollar"
    "SGD" -> "SG Dollar"
    "KRW" -> "Won"
    "BRL" -> "Real"
    "MXN" -> "Peso"
    else -> CurrenciesCatalog.byCode(code)?.fullCountryName ?: code
}

/** KMP-safe 10^n for small integer exponents. */
private fun pow10(exponent: Int): Double {
    var result = 1.0
    repeat(exponent) { result *= 10.0 }
    return result
}
