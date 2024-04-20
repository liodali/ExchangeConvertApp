package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.compose.dialog.BodyCurrenciesBottomSheet
import dali.hamza.echangecurrencyapp.viewmodel.DialogCurrencyViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun SelectCurrencyPage(
    modifier: Modifier,
    onSelect: (String) -> Unit
) {

    Scaffold(topBar = {
        SelectCurrencyTopBar()
    }, modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
        ) {
            BodyCurrenciesBottomSheet(
                modifier = Modifier.weight(0.9f)
            )
            SelectCurrencyButton(onSelect = onSelect, modifier = Modifier.weight(0.1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCurrencyTopBar(
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.selectTitleCurrencyTopBarPage)) },
        modifier = Modifier.then(modifier),
    )
}

@Composable
fun SelectCurrencyButton(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
    viewModel: DialogCurrencyViewModel = koinViewModel<DialogCurrencyViewModel>()
) {
    val state = viewModel.getCurrentCurrency()
        .collectAsState()

    Button(
        onClick = {
            viewModel.mutableFlowSearchCurrency = ""
            viewModel.setPreferenceCurrency(viewModel.getCurrentCurrency().value)
            onSelect(viewModel.getCurrentCurrency().value)
        },
        enabled = state.value.isNotEmpty(),
        modifier = modifier

            .requiredHeight(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = stringResource(id = R.string.continueLabel))
    }
}