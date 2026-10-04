package dali.hamza.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerLineChart
import dali.hamza.shared.ui.components.LedgerLogoMark
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.HistoryRange
import dali.hamza.shared.ui.viewmodel.HistoryViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Exchange Rates page (design frame `2XSA3` — Home Page / Rates Display) —
 * pushed converter route.
 *
 * Sections: header, Quick Exchange card (amount + from/to selectors + result
 * + Convert — recording into the Phase 4 ledger via `SharedViewModel.convert`),
 * Price History (30-day chart for the active pair), and the Available Rates
 * grid with day-over-day change chips.
 */
@Composable
fun ConverterCurrencyScreen(
    viewModel: SharedViewModel,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenFullConverter: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val historyState by historyViewModel.state.collectAsState()
    var pickerFor by remember { mutableStateOf<Boolean?>(null) } // true=from, false=to

    val base = state.fromCurrency?.name
    val quote = state.toCurrency?.name

    // 30-day trend for the active pair
    LaunchedEffect(base, quote) {
        if (base != null && quote != null && base != quote) {
            historyViewModel.load(base, quote, HistoryRange.ONE_MONTH)
        }
    }
    // day-over-day deltas for the rates grid
    LaunchedEffect(base, state.rates) {
        if (base != null && state.rates.isNotEmpty()) {
            historyViewModel.loadGridDeltas(base, state.rates.map { it.name })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(
            title = "SOVEREIGN LEDGER",
            onBack = onBack,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // ============ Header ===========================================
            Text(
                text = "Exchange Rates",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Live rates based on ${base ?: "—"} base currency",
                style = MaterialTheme.typography.bodyLarge,
                color = LedgerColors.TextSecondary,
            )

            Spacer(Modifier.height(24.dp))

            // ============ Quick Exchange ===================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Exchange",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = LedgerColors.TextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Convert currencies using live rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
                Text(
                    text = "FULL CONVERTER",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LedgerColors.Blue,
                    modifier = Modifier.clickable(onClick = onOpenFullConverter),
                )
            }
            Spacer(Modifier.height(16.dp))

            BentoCard(fill = LedgerColors.SurfaceElevated, cornerRadius = 16.dp) {
                // ---- amount -----------------------------------------------
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = LedgerColors.TextTertiary,
                )
                Spacer(Modifier.height(8.dp))
                AmountField(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                )

                Spacer(Modifier.height(20.dp))

                // ---- from / to selectors ---------------------------------
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencySelector(
                        code = base ?: "—",
                        modifier = Modifier.weight(1f),
                        onClick = { pickerFor = true },
                    )
                    CurrencySelector(
                        code = quote ?: "—",
                        modifier = Modifier.weight(1f),
                        onClick = { pickerFor = false },
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ---- result ----------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LedgerColors.CardAlt.copy(alpha = 0.35f))
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Converted Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.convertedAmount?.let { fixed(it, 2) } ?: "0.00",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = LedgerColors.TextPrimary,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ---- convert ---------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LedgerColors.Green.copy(alpha = 0.20f))
                        .clickable(enabled = state.convertedAmount != null) { viewModel.convert() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Convert",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LedgerColors.Green,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ============ Price history ===================================
            Text(
                text = "Price History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "30 day trend — ${base ?: "—"} to ${quote ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = LedgerColors.TextTertiary,
            )
            Spacer(Modifier.height(16.dp))
            BentoCard(fill = LedgerColors.SurfaceElevated, cornerRadius = 16.dp) {
                if (historyState.series.size >= 2) {
                    LedgerLineChart(
                        points = historyState.series.map { it.rate },
                        labels = historyState.series.map { it.date },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(192.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(192.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LedgerColors.Card.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Loading price history…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LedgerColors.TextTertiary,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = historyState.series.firstOrNull()?.date ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = LedgerColors.TextSecondary.copy(alpha = 0.50f),
                )
                Text(
                    text = historyState.series.lastOrNull()?.date ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = LedgerColors.TextSecondary.copy(alpha = 0.50f),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ Available rates ================================
            Text(
                text = "Available Rates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = LedgerColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.rates.forEach { rate ->
                    val name = state.currencies
                        .firstOrNull { it.name == rate.name }
                        ?.fullCountryName
                        ?: ""
                    RateGridCard(
                        rate = rate,
                        currencyName = name,
                        deltaPercent = historyState.gridDeltas[rate.name],
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
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

// ---------------------------------------------------------------------------
// pieces
// ---------------------------------------------------------------------------

/** Amount input — card fill, radius 12, numeric keyboard, no autofocus. */
@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LedgerColors.Card)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = LedgerColors.TextPrimary,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(LedgerColors.Blue),
            decorationBox = { field ->
                if (value.isEmpty()) {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        ),
                        color = LedgerColors.TextTertiary,
                    )
                }
                field()
            },
        )
    }
}

/** From/To selector chip — card fill, radius 12, code + chevron. */
@Composable
private fun CurrencySelector(
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LedgerColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LedgerColors.TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = LedgerColors.TextTertiary,
        )
    }
}

/** One "Available Rates" card — code + name on the left, rate + delta right. */
@Composable
private fun RateGridCard(
    rate: ExchangeRate,
    currencyName: String,
    deltaPercent: Double?,
) {
    BentoCard(fill = LedgerColors.SurfaceElevated, cornerRadius = 16.dp, padding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rate.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    color = LedgerColors.TextPrimary,
                )
                if (currencyName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = currencyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = fixed(rate.rate, 2),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    color = LedgerColors.TextPrimary,
                )
                if (deltaPercent != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = signedPercent(deltaPercent),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (deltaPercent >= 0) LedgerColors.Green else LedgerColors.Error,
                    )
                }
            }
        }
    }
}

private fun fixed(value: Double, precision: Int): String {
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val rounded = kotlin.math.round(value * factor) / factor
    var text = rounded.toString()
    if (!text.contains('.')) text += ".0"
    while (text.substringAfter('.').length < precision) text += "0"
    return text
}

private fun signedPercent(value: Double): String {
    val sign = if (value >= 0) "+" else "-"
    return "$sign${fixed(kotlin.math.abs(value), 2)}%"
}
