package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.ui.MainActivity.Companion.mainViewModelComposition
import dali.hamza.echangecurrencyapp.ui.compose.component.CurrencySelectionCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.compose.component.HeaderHomeCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.SpacerHeight

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun Home(
    openFragment: () -> Unit
) {
    val viewModel = MainActivity.mainViewModelComposition.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.Home))
                },
                actions = {
                    if(!viewModel.showFormAmount){
                        TextButton(onClick = {
                            viewModel.showFormAmount = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "",
                                tint = Color.White
                            )
                        }
                    }

                }
            )
        }
    ) {
        BodyHomeCompose(openFragment = openFragment)
    }

}

@ExperimentalComposeUiApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BodyHomeCompose(openFragment: () -> Unit) {

    Column() {
        HeaderHomeCompose(
            openFragment = openFragment
        )
        SpacerHeight(
            height = 24.dp
        )
        ExchangesRatesGrid()
    }
}



