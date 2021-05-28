package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.MainActivity.Companion.mainViewModelComposition
import dali.hamza.echangecurrencyapp.ui.compose.component.CurrencySelectionCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.compose.component.SpacerHeight

@ExperimentalMaterialApi
@Composable
fun Home(
    openFragment: () -> Unit
) {
    val viewModel = mainViewModelComposition.current

    Scaffold() {
        Column() {
            InputAmount(
                {
                    viewModel.changeAmount(it)
                },
                form = viewModel.mutableFlowAutoWalletForm
            )
            SpacerHeight(
                height = 8.dp
            )
            CurrencySelectionCompose(
                openFragment = openFragment
            )
            SpacerHeight(
                height = 8.dp
            )
            ExchangesRatesGrid(
                rates = (1..20).map {
                    "$it"
                }
            )
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputAmount(
    onValueChanged: (String) -> Unit,
    form: AmountInput,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.then(
            Modifier
                .verticalScroll(ScrollState(0))
                .padding(horizontal = 5.dp)
        )
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        SpacerHeight(
            height = 8.dp
        )
        TextField(
            form.amount,
            onValueChanged,
            label = {
                Text(stringResource(id = R.string.amount_label))
            },
            trailingIcon = {
                showCurrencySelected()
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),

            )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun showCurrencySelected() {
    val viewModel = mainViewModelComposition.current
    if (viewModel.getCurrencySelection().isNotEmpty()) {
        Text(text = viewModel.getCurrencySelection())
    }
}

