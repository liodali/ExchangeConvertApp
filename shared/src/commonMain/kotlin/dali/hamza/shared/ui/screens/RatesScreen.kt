package dali.hamza.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.common.formatString
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.ui.components.CurrencyPickerSheet
import dali.hamza.shared.ui.viewmodel.SharedViewModel

/**
 * Exchange rates screen (shared by Android and iOS).
 * Replaces the SwiftUI RatesView + RateCardView.
 */
@Composable
fun RatesScreen(
    viewModel: SharedViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    var showBasePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Exchange rates",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "1 ${state.fromCurrency?.name ?: ""} =",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${state.fromCurrency?.name ?: "USD"} ▾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showBasePicker = true }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::refresh) {
                    Text(text = "⟳", fontSize = 20.sp)
                }
            }
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        val amount = state.amount.toDoubleOrNull()
        // Group the flat rates list by first letter (A, B, C …) like the Android app
        val grouped = remember(state.rates) {
            state.rates.sortedBy { it.name }.groupBy { it.name.firstOrNull()?.toString() ?: "" }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (letter, rates) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(rates, key = { it.name }) { exchangeRate ->
                    RateCard(
                        exchangeRate = exchangeRate,
                        baseCurrency = state.fromCurrency?.name ?: "",
                        amount = amount,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }            }
        }
    }

    CurrencyPickerSheet(
        visible = showBasePicker,
        currencies = state.currencies,
        title = "Select base currency",
        onDismiss = { showBasePicker = false },
        onCurrencySelected = viewModel::selectFromCurrency
    )
}

/**
 * Rate display card (shared replacement of RateCardView).
 */
@Composable
private fun RateCard(
    exchangeRate: ExchangeRate,
    baseCurrency: String,
    amount: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exchangeRate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "1 $baseCurrency = ${formatString("%.4f", exchangeRate.rate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatString("%.4f", exchangeRate.rate * (amount ?: 1.0)),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
