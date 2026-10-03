package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_button_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_card_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_background
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_on_surface_muted
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_stroke
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_success
import dali.hamza.echangecurrencyapp.ui.compose.theme.design_surface_variant

/**
 * QuickExchangeSection component for currency conversion
 * Uses API rates for calculation: result = amount * (rates[toCurrency] / rates[fromCurrency])
 *
 * @param baseCurrency The current base currency from API
 * @param rates Map of currency codes to their exchange rates from API
 * @param onConvertClick Callback when convert button is clicked
 * @param onFromCurrencyClick Callback to open currency picker for "from" currency
 * @param onToCurrencyClick Callback to open currency picker for "to" currency
 */
@Composable
fun QuickExchangeSection(
    baseCurrency: String = "USD",
    rates: Map<String, Double> = emptyMap(),
    onConvertClick: (Double, String, String) -> Unit = { _, _, _ -> },
    onFromCurrencyClick: () -> Unit = {},
    onToCurrencyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf(baseCurrency) }
    var toCurrency by remember { mutableStateOf("EUR") }
    var result by remember { mutableStateOf<String?>(null) }
    
    // Calculate conversion when amount or currencies change
    fun calculateConversion() {
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        if (amountValue > 0 && rates.isNotEmpty()) {
            // Formula: amount * (rates[toCurrency] / rates[fromCurrency])
            // If fromCurrency is base, use rate directly
            val fromRate = if (fromCurrency == baseCurrency) 1.0 else rates[fromCurrency] ?: 1.0
            val toRate = if (toCurrency == baseCurrency) 1.0 else rates[toCurrency] ?: 1.0
            val convertedAmount = amountValue * (toRate / fromRate)
            result = String.format("%.2f", convertedAmount)
        } else {
            result = "0.00"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = design_card_background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(design_card_background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Exchange",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_on_background
                )
                Text(
                    text = "Convert currencies using live rates",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = design_on_surface_muted
                )
            }

            // Amount Input
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Amount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = design_on_surface_muted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(design_surface_variant)
                        .background(
                            Color(0xFF44474d).copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = amount.ifEmpty { "0.00" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = design_on_background
                    )
                }
            }

            // Currency Row (From/To)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // From Currency
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExchangeButton(
                        currency = fromCurrency,
                        onClick = onFromCurrencyClick
                    )
                }

                // To Currency
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExchangeButton(
                        currency = toCurrency,
                        onClick = onToCurrencyClick
                    )
                }
            }

            // Result Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(design_button_background.copy(alpha = 0.2f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Converted Amount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = design_on_surface_muted
                )
                Text(
                    text = result ?: "0.00",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_on_background
                )
            }

            // Convert Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(design_success.copy(alpha = 0.2f))
                    .clickable {
                        calculateConversion()
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        onConvertClick(amountValue, fromCurrency, toCurrency)
                    }
                    .background(design_success, RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Convert",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = design_success
                )
            }
        }
    }
}

@Composable
private fun ExchangeButton(
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(design_surface_variant)
            .background(
                Color(0xFF44474d).copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = design_on_background
        )
        // Dropdown arrow could be added here
        Text(
            text = "▼",
            fontSize = 10.sp,
            color = design_on_surface_muted
        )
    }
}
