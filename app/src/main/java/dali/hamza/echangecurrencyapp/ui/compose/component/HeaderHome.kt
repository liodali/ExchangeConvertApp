package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeaderHomeCompose(
    openFragment: () -> Unit
) {
    val viewModel = koinViewModel<MainViewModel>()
    InputAmount(
        openCurrencyDialog = openFragment,
        actionCalculate =
        if (viewModel.getCurrencySelection().value.isNotEmpty()) {
            {
                val amount = viewModel.mutableFlowAmountForm.amount
                if (amount.isNotEmpty() && amount.toDouble() > 0.0) {
                    viewModel.isLoading = true
                    viewModel.showFormAmount = false
                    viewModel.calculateExchangeRates(viewModel.mutableFlowAmountForm.amount.toDouble())
                }

            }
        } else null,
        onValueChanged = { amount ->
            viewModel.changeAmount(amount)
        },
        clearText = {
            viewModel.changeAmount("")
        },
        form = viewModel.mutableFlowAmountForm,
        currency = viewModel.getCurrencySelection().value,
        keyboardController = LocalSoftwareKeyboardController.current,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    )

}
