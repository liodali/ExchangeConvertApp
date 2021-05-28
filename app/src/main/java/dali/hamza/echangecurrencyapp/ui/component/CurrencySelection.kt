package dali.hamza.echangecurrencyapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.mainViewModelComposition

@Composable
fun CurrencySelectionCompose() {
    val viewModel = mainViewModelComposition.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            end = 8.dp
        ),
        horizontalArrangement = Arrangement.End
    ) {
        if (viewModel.getCurrencySelection().isEmpty())
            Button(onClick = { }) {
                Text(text = stringResource(id = R.string.select_currency_label_bt))
            }
    }
}