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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.SharedViewModel

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
    onOpenConverter: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var pickerFor by remember { mutableStateOf<Boolean?>(null) } // true=from, false=to, null=hidden

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
            MarketOverviewSection(base = state.fromCurrency, rates = state.rates.topPairs())

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
            RecentActivitySection()

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
}

// ------------------------------------------------------ 1. market overview

/** Top pairs — majors first, MAD included (design shows EUR/BTC/XAU rows). */
private val TOP_PAIRS = listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD")

private fun List<ExchangeRate>.topPairs(): List<ExchangeRate> =
    TOP_PAIRS.mapNotNull { symbol -> firstOrNull { it.name == symbol } }

@Composable
private fun MarketOverviewSection(base: Currency?, rates: List<ExchangeRate>) {
    SectionHeader(title = "Market Overview", icon = Icons.Outlined.CurrencyExchange)
    Spacer(Modifier.height(16.dp))

    if (rates.isEmpty()) {
        BentoCard(fill = LedgerColors.Card) {
            Text(
                text = "Loading live rates…",
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerColors.TextTertiary,
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rates.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { rate ->
                    PairCard(
                        quote = rate.name,
                        quoteName = shortCurrencyName(rate.name),
                        base = base?.name ?: "USD",
                        rate = rate.rate,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Design "EUR/USD Card": pair title (Manrope 700/18) + full name (Inter 12)
 * + LIVE chip + big rate (Manrope 700/30). The +% change badge and mini
 * chart arrive with Phase 4 (/historical).
 */
@Composable
private fun PairCard(
    quote: String,
    quoteName: String,
    base: String,
    rate: Double,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier,
        fill = LedgerColors.Card,
        padding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // pair label on two lines: quote / (line 1), base (line 2)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$quote /",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = LedgerColors.TextPrimary,
                )
                Text(
                    text = base,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = LedgerColors.TextPrimary,
                )
                Text(
                    text = quoteName,
                    style = MaterialTheme.typography.bodySmall,
                    color = LedgerColors.TextTertiary,
                )
            }
            LiveChip()
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = formatLedgerNumber(rate),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            ),
            color = LedgerColors.TextPrimary,
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
private fun RecentActivitySection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SectionHeader(
            title = "Recent Activity",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        )
    }
    Spacer(Modifier.height(16.dp))

    BentoCard(fill = LedgerColors.Card) {
        EmptyState(
            title = "No activity yet",
            message = "Your exchanges will appear here once recorded (Phase 4).",
            icon = Icons.Outlined.History,
        )
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
