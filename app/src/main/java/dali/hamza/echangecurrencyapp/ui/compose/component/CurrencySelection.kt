package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.MainActivity.Companion.mainViewModelComposition
import kotlinx.coroutines.launch

@Composable
fun CurrencySelectionCompose(
    openFragment: () -> Unit
) {
    val viewModel = mainViewModelComposition.current
    val currency = viewModel.getCurrencySelection()
    val scope = rememberCoroutineScope()
    when (currency != null && currency.isEmpty()) {
        true -> Button(onClick = {
            scope.launch {
                openFragment()
            }
        }) {
            Text(text = stringResource(id = R.string.select_currency_label_bt))
        }
        else ->
            EmptyBox()
    }

}