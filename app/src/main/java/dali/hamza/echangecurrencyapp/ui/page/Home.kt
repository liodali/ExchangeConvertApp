package dali.hamza.echangecurrencyapp.ui.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.mainViewModelComposition
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.component.CurrencySelectionCompose
import dali.hamza.echangecurrencyapp.ui.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.component.SpacerHeight
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel

@ExperimentalMaterialApi
@Composable
fun Home() {
    val viewModel = mainViewModelComposition.current

    Scaffold() {
        Column() {
            FormCreateAutoWallet(
                {
                    viewModel.changeAmount(it)
                },
                form = viewModel.mutableFlowAutoWalletForm
            )
            SpacerHeight(
                height = 8.dp
            )
            CurrencySelectionCompose()
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

@Composable
fun FormCreateAutoWallet(
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
        SpacerHeight(
            height = 8.dp
        )
        TextField(
            form.amount,
            onValueChanged,
            label = {
                Text(stringResource(id = R.string.amount_label))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        )
    }
}


