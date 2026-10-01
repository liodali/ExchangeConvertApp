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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.ui.components.BentoCard
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.components.LedgerButton
import dali.hamza.shared.ui.components.LedgerInput
import dali.hamza.shared.ui.components.LedgerTopAppBar
import dali.hamza.shared.ui.theme.LedgerColors
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Full converter (pushed route from the Exchange Rates page) — the focused,
 * Sovereign-styled conversion surface: amount, from/to selectors with swap,
 * live rate line, large ledger result, and Execute Exchange (which records
 * the conversion into the Phase 4 ledger).
 */
@Composable
fun FullConverterScreen(
    viewModel: SharedViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var pickerFor by remember { mutableStateOf<Boolean?>(null) } // true=from, false=to

    val base = state.fromCurrency
    val quote = state.toCurrency
    val rate = viewModel.currentRate()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LedgerTopAppBar(title = "SOVEREIGN LEDGER", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Currency Converter",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = LedgerColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Execute a conversion at the live market rate",
                style = MaterialTheme.typography.bodyLarge,
                color = LedgerColors.TextSecondary,
            )

            Spacer(Modifier.height(24.dp))

            BentoCard(fill = LedgerColors.SurfaceElevated) {
                LedgerInput(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = "AMOUNT",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Spacer(Modifier.height(24.dp))

                // ---- from / swap / to -------------------------------------
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BigCurrencySelector(
                        label = "FROM",
                        code = base?.name,
                        fullName = base?.fullCountryName,
                        modifier = Modifier.weight(1f),
                        onClick = { pickerFor = true },
                    )
                    IconButton(onClick = viewModel::swapCurrencies) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Swap currencies",
                            tint = LedgerColors.Blue,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    BigCurrencySelector(
                        label = "TO",
                        code = quote?.name,
                        fullName = quote?.fullCountryName,
                        modifier = Modifier.weight(1f),
                        onClick = { pickerFor = false },
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ---- live rate line ---------------------------------------
                if (rate != null && base != null && quote != null) {
                    Text(
                        text = "1 ${base.name} = ${formatRateValue(rate)} ${quote.name}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = LedgerColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ---- result -----------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LedgerColors.Canvas)
                        .padding(24.dp),
                ) {
                    Text(
                        text = when {
                            state.isLoading -> "…"
                            state.convertedAmount != null -> formatRateValue(state.convertedAmount!!)
                            else -> "—"
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                        ),
                        color = if (state.convertedAmount != null) LedgerColors.Green else LedgerColors.TextTertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(24.dp))

                LedgerButton(
                    text = "Execute Exchange",
                    onClick = viewModel::convert,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.convertedAmount != null,
                )
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

/** Large from/to selector — code + full name + chevron. */
@Composable
private fun BigCurrencySelector(
    label: String,
    code: String?,
    fullName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LedgerColors.TextTertiary,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LedgerColors.Canvas)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = code ?: "—",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LedgerColors.TextPrimary,
                )
                if (!fullName.isNullOrBlank()) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = LedgerColors.TextTertiary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = LedgerColors.TextTertiary,
            )
        }
    }
}

private fun formatRateValue(value: Double): String {
    val precision = if (kotlin.math.abs(value) >= 1.0) 4 else 6
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val rounded = kotlin.math.round(value * factor) / factor
    return rounded.toString()
}
