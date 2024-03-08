package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.flow.asStateFlow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeaderHomeCompose(
    openFragment: () -> Unit,
    viewModel :MainViewModel = koinViewModel<MainViewModel>()
) {
    val amountForm = viewModel.mutableStateAmountForm
    InputAmount(
        openCurrencyDialog = openFragment,
        actionCalculate =
        if (viewModel.hasCurrencySelection()) {
            {
                val amount = amountForm.amount
                if (amount.isNotEmpty() && amount.toDouble() > 0.0) {
                    viewModel.isLoading = true
                    viewModel.showFormAmount = false
                    viewModel.calculateExchangeRates(amount.toDouble())
                }

            }
        } else null,
        onValueChanged = { amount ->
            viewModel.changeAmount(amount)
        },
        clearText = {
            viewModel.changeAmount("")
        },
        form = amountForm,
        currency = viewModel.getCurrencySelection().value,
        keyboardController = LocalSoftwareKeyboardController.current,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    )

}
