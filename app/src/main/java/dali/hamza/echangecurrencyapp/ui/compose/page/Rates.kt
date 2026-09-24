package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.echangecurrencyapp.models.DataUIState
import dali.hamza.echangecurrencyapp.models.LoadingUIState
import dali.hamza.echangecurrencyapp.models.NoDataUIState
import dali.hamza.echangecurrencyapp.models.UIState
import dali.hamza.echangecurrencyapp.ui.compose.component.HeaderHomeCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.HistoryGraphSection
import dali.hamza.echangecurrencyapp.ui.compose.component.QuickExchangeSection
import dali.hamza.echangecurrencyapp.ui.compose.component.RateCard
import dali.hamza.echangecurrencyapp.ui.compose.component.StateBuilder
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * RatesPageCompose - Main entry point for the Rates page
 * Displays:
 * - Header with currency selector
 * - Quick Exchange section (API-driven)
 * - History Graph section (mock data)
 * - Rates list (API-driven)
 */
@ExperimentalComposeUiApi
@Composable
fun RatesPageCompose(
    modifier: Modifier = Modifier,
    openFragment: () -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val ratesState: UIState = viewModel.getExchangeRates()
        .collectAsStateWithLifecycle().value

    val baseCurrency by viewModel.getCurrencySelection()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Header with currency selector
        HeaderHomeCompose(
            openFragment = openFragment
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Quick Exchange Section
            item {
                QuickExchangeSection(
                    baseCurrency = baseCurrency,
                    rates = emptyMap(), // TODO: Pass actual rates from API
                    onConvertClick = { amount, from, to ->
                        // TODO: Handle conversion and navigate to result or show snackbar
                    },
                    onFromCurrencyClick = {
                        openFragment() // Open currency picker
                    },
                    onToCurrencyClick = {
                        openFragment() // Open currency picker
                    }
                )
            }

            // History Graph Section (mock data)
            item {
                HistoryGraphSection(
                    baseCurrency = baseCurrency,
                    targetCurrency = "EUR" // TODO: Make selectable
                )
            }

            // Rates Header
            item {
                Text(
                    text = "Available Rates",
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFFe5e2e1),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }


            // Rates List (API-driven)
            when (ratesState) {
                is LoadingUIState -> {
                    item {
                        Box {
                            // TODO: Add loading indicator
                        }
                    }
                }

                is NoDataUIState -> {
                    item {
                        Box {
                            // TODO: Add empty state
                        }
                    }
                }


                is DataUIState<*> -> {
                    items((ratesState as DataUIState<List<ExchangeRate>>).data) { rate ->
                        RateCard(
                            currencyCode = rate.name,
                            currencyName = getCurrencyName(rate.name),
                            rateValue = rate.calculatedAmount,
                            changePercent = null, // TODO: Calculate from historic data
                            modifier = Modifier.padding(horizontal = 0.dp)
                        )
                    }
                }

                else -> {
                    item {
                        Box {
                            // TODO: Add empty state
                        }
                    }
                }
            }
            ratesState.StateBuilder<List<ExchangeRate>>(
                loadingUI = {
                    Box {
                        // TODO: Add loading indicator
                    }
                },
                emptyUI = {

                }
            ) { data ->

            }


            // Footer with last updated
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Last updated: Just now",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    color = androidx.compose.ui.graphics.Color(0xFF8f9097),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Helper function to get currency name from code
 * TODO: Replace with actual currency data from API
 */
private fun getCurrencyName(code: String): String {
    return when (code.uppercase()) {
        "EUR" -> "Euro"
        "GBP" -> "British Pound"
        "JPY" -> "Japanese Yen"
        "CHF" -> "Swiss Franc"
        "CAD" -> "Canadian Dollar"
        "AUD" -> "Australian Dollar"
        "USD" -> "US Dollar"
        else -> code
    }
}
