package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.ui.compose.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.compose.component.HeaderHomeCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.SpacerHeight
import dali.hamza.echangecurrencyapp.ui.compose.dialog.BottomSheetCurrencies
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun Home(
    openFragment: () -> Unit
) {
    val viewModel = MainActivity.mainViewModelComposition.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetScaffoldState()
    var showBottomSheet by remember { mutableStateOf(false) }

    BottomSheetScaffold(
        sheetContent = {
            BottomSheetCurrencies(onClose = {
                scope.launch {
                    sheetState.bottomSheetState.hide()
                }
            }, onSelect = { currency ->
                viewModel.setCurrencySelection(currency)

            })
        },
        scaffoldState = sheetState,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.Home))
                },
                actions = {
                    if (!viewModel.showFormAmount) {
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
        },
    ) { innerPadding ->
        BodyHomeCompose(
            modifier = Modifier
                .padding(innerPadding),
            openFragment = {
                scope.launch {
                    sheetState.bottomSheetState.show()
                }
            }
        )

    }

}

@ExperimentalComposeUiApi
@Composable
fun BodyHomeCompose(
    modifier: Modifier,
    openFragment: () -> Unit
) {

    Column(modifier = modifier) {
        HeaderHomeCompose(
            openFragment = openFragment
        )
        SpacerHeight(
            height = 24.dp
        )
        ExchangesRatesGrid()
    }
}



