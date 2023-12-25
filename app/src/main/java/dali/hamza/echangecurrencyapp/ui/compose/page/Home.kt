package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import dali.hamza.echangecurrencyapp.ui.compose.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.compose.component.HeaderHomeCompose
import dali.hamza.echangecurrencyapp.ui.compose.component.SpacerHeight
import dali.hamza.echangecurrencyapp.ui.compose.dialog.BottomSheetCurrencies
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun Home(
    openFragment: () -> Unit
) {

    val viewModel = koinViewModel<MainViewModel>()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.Home))
                }
            )
        },
    ) { innerPadding ->
        BodyHomeCompose(
            modifier = Modifier
                .padding(innerPadding),
            openFragment = {
                scope.launch {
                    //sheetState.show()
                    showBottomSheet = true
                }
            }
        )
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch {
                        sheetState.hide()
                    }
                    showBottomSheet = false
                },
                tonalElevation = 6.dp,
                sheetState = sheetState,
                scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp),
            ) {
                BottomSheetCurrencies(
                    onClose = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    onSelect = { currency ->
                        viewModel.setCurrencySelection(currency)
                        scope.launch {
                            sheetState.hide()
                        }
                        showBottomSheet = false
                    },
                )


            }
        }

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



