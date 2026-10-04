package dali.hamza.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.shared.domain.models.Currency

/**
 * Searchable currency picker, hosted in a modal bottom sheet
 * (shared replacement of the Android [CurrenciesPicker] dialog and the inline
 * SwiftUI picker inside RatesView).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    visible: Boolean,
    currencies: List<Currency>,
    title: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (Currency) -> Unit,
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }

    val filtered = remember(query, currencies) {
        if (query.isBlank()) {
            currencies
        } else {
            currencies.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.fullCountryName.contains(query, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                items(filtered) { currency ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCurrencySelected(currency)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = currency.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text(
                            text = currency.fullCountryName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
