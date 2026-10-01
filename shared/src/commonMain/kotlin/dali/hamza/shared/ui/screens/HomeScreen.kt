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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.EmptyState
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerButtonVariant
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.components.SectionHeader
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import dali.hamza.shared.ui.viewmodel.SharedUiState

/**
 * Sovereign Market Dashboard (design frame `RbQhR`) — Home tab.
 *
 * - Market overview: top pairs for the session base currency, from the
 *   existing [SharedViewModel] rates (single source of truth — no duplicate
 *   fetching or conversion logic; the design-migration contract).
 * - Quick Exchange: the existing conversion flow (amount, from/to pickers,
 *   swap, convert).
 * - Recent activity: placeholder until the transactions table (Phase 4).
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
            title = "Sovereign Ledger",
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

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            // ---- Market overview -------------------------------------------
            SectionHeader(title = "Market Overview", icon = Icons.Outlined.CurrencyExchange)
            MarketOverview(base = state.fromCurrency, rates = state.rates.topPairs())
            Spacer(Modifier.height(24.dp))

            // ---- Quick Exchange ---------------------------------------------
            SectionHeader(title = "Quick Exchange", icon = Icons.Outlined.SwapHoriz)
            QuickExchangeCard(
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
                onConvert = viewModel::convert,
                onOpenConverter = onOpenConverter,
            )
            Spacer(Modifier.height(24.dp))

            // ---- Recent activity (placeholder until Phase 4) ------------------
            SectionHeader(title = "Recent Activity", icon = Icons.AutoMirrored.Outlined.ReceiptLong)
            BentoCard(fill = LedgerColors.Card) {
                EmptyState(
                    title = "No activity yet",
                    message = "Your conversion history will appear here.",
                    icon = Icons.Outlined.History,
                )
            }

            Spacer(Modifier.height(120.dp))
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

// ------------------------------------------------------------------ overview

/** Top pairs shown in the overview — majors first, MAD included (design). */
private val TOP_PAIRS = listOf("EUR", "GBP", "MAD", "JPY", "CHF", "CAD")

private fun List<ExchangeRate>.topPairs(): List<ExchangeRate> =
    TOP_PAIRS.mapNotNull { symbol -> firstOrNull { it.name == symbol } }

@Composable
private fun MarketOverview(base: Currency?, rates: List<ExchangeRate>) {
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
                        base = base?.name ?: "—",
                        symbol = rate.name,
                        rate = rate.rate,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PairCard(
    base: String,
    symbol: String,
    rate: Double,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier,
        fill = LedgerColors.Card,
        padding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$base / $symbol",
                style = MaterialTheme.typography.labelMedium,
                color = LedgerColors.TextSecondary,
            )
            Spacer(Modifier.width(8.dp))
            LiveDot()
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = formatLedgerNumber(rate),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = LedgerColors.TextPrimary,
        )
    }
}

@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live-alpha",
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(LedgerColors.Green)
    )
}

// ------------------------------------------------------------- quick exchange

@Composable
private fun QuickExchangeCard(
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
    onConvert: () -> Unit,
    onOpenConverter: () -> Unit,
) {
    BentoCard(fill = LedgerColors.NavyPanel) {
        LedgerInput(
            value = amount,
            onValueChange = onAmountChange,
            label = "Amount",
            placeholder = "0.00",
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

        LedgerButton(
            text = "Convert",
            onClick = onConvert,
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
        Column {
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
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
        )
    }
}

// ------------------------------------------------------------------ helpers

/**
 * Ledger-style number formatting (KMP-safe, locale-stable):
 * fixed precision by magnitude — >=100: 2dp · >=1: 4dp · <1: 6dp.
 */
internal fun formatLedgerNumber(value: Double): String {
    val precision = when {
        value >= 100.0 -> 2
        value >= 1.0 -> 4
        else -> 6
    }
    val factor = pow10(precision)
    val rounded = kotlin.math.round(value * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}

/** KMP-safe 10^n for small integer exponents (no kotlin.math.pow on all targets). */
private fun pow10(exponent: Int): Double {
    var result = 1.0
    repeat(exponent) { result *= 10.0 }
    return result
}
