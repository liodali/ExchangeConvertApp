package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.MainActivity

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputAmount(
    openCurrencyDialog: () -> Unit,
    actionCalculate: (() -> Unit)?,
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
                showCurrencySelected(openCurrencyDialog)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (actionCalculate != null)
                        actionCalculate()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),

            )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(
                    align = Alignment.End
                ),
            horizontalArrangement = Arrangement.End
        ) {
            CurrencySelectionCompose(
                openFragment = openCurrencyDialog
            )
            SpacerWidth(
                width = 2.dp
            )
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (actionCalculate != null)
                        actionCalculate()
                },
                // enabled = form.amount.isNotEmpty() && form.amount.toDouble()>0.0
            ) {
                Text(text = stringResource(id = R.string.convertLabel))

            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun showCurrencySelected(
    openCurrencyDialog: () -> Unit,
) {
    val viewModel = MainActivity.mainViewModelComposition.current
    val currency = viewModel.getCurrencySelection()
    when (currency != null && currency.isNotEmpty()) {
        true -> Text(text = currency, modifier = Modifier.clickable {
            openCurrencyDialog()
        })
        else -> EmptyBox()
    }
}
