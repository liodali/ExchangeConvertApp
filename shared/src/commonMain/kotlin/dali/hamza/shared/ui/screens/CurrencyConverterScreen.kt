package dali.hamza.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate

@Composable
fun CurrencyConverterScreen(
    currencies: List<Currency>,
    exchangeRates: List<ExchangeRate>,
    selectedFromCurrency: Currency?,
    selectedToCurrency: Currency?,
    amount: String,
    onAmountChange: (String) -> Unit,
    onFromCurrencySelected: (Currency) -> Unit,
    onToCurrencySelected: (Currency) -> Unit,
    onSwapCurrencies: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // From Currency Selection
        CurrencySelector(
            label = "From",
            selectedCurrency = selectedFromCurrency,
            currencies = currencies,
            onCurrencySelected = onFromCurrencySelected,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Swap Button
        IconButton(
            onClick = onSwapCurrencies,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "⇅",
                fontSize = 24.sp
            )
        }
        
        // To Currency Selection
        CurrencySelector(
            label = "To",
            selectedCurrency = selectedToCurrency,
            currencies = currencies,
            onCurrencySelected = onToCurrencySelected,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Exchange Rate Result
        if (exchangeRates.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Exchange Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    exchangeRates.forEach { rate ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = rate.name)
                            Text(
                                text = String.format("%.4f", rate.calculatedAmount),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    selectedCurrency: Currency?,
    currencies: List<Currency>,
    onCurrencySelected: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedCurrency?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.name} - ${currency.fullCountryName}") },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}
