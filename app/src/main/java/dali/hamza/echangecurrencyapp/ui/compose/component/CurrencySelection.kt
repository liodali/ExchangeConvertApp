package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.MainActivity.Companion.mainViewModelComposition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CurrencySelectionCompose(
    openFragment :()-> Unit

) {
    val viewModel = mainViewModelComposition.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                end = 8.dp
            ),
        horizontalArrangement = Arrangement.End
    ) {
        if (viewModel.getCurrencySelection().isEmpty())
            Button(onClick = {
                scope.launch {
                    openFragment()
                }
            }) {
                Text(text = stringResource(id = R.string.select_currency_label_bt))
            }
    }
}