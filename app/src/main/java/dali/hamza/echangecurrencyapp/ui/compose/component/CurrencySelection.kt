package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun CurrencySelectionCompose(
    openFragment: () -> Unit
) {
    val viewModel = koinViewModel<MainViewModel>()
    val currency = viewModel.getCurrencySelection().value
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