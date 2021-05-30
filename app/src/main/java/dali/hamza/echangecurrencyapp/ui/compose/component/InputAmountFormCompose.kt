package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
        Button(
            onClick = {
                keyboardController?.hide()
                if (actionCalculate != null)
                    actionCalculate()
            },
            modifier = Modifier.wrapContentWidth(
                align = Alignment.End
            ),
            enabled = actionCalculate != null
        ) {
            Text(text = stringResource(id = R.string.converLabel))

        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun showCurrencySelected(
    openCurrencyDialog: () -> Unit,
) {
    val viewModel = MainActivity.mainViewModelComposition.current
    if (viewModel.getCurrencySelection().isNotEmpty()) {
        Text(text = viewModel.getCurrencySelection(), modifier = Modifier.clickable {
            openCurrencyDialog()
        })
    }
}
