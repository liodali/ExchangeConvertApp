package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.util.Date

@Suppress("UNCHECKED_CAST")
@ExperimentalComposeUiApi
@Composable
fun ExchangesRatesGrid(viewModel: MainViewModel = koinViewModel<MainViewModel>()) {
    val ratesState = viewModel.getExchangeRates().collectAsState().value
    ratesState.StateBuilder<List<ExchangeRate>>(
        loadingUI = { Loading() },
        emptyUI = { EmptyBox() }) { data ->
        ShowListRates(
            rates = data,
            currentCurrency = viewModel.getCurrencySelection().value
        )
    }
}


@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun ShowDataListRates(
    rates: List<ExchangeRate>,
    currentCurrency: String,

) {

    val rememberRates = rememberSaveable {
        mutableStateOf(rates)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 0.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            Modifier.fillMaxWidth()
        ) {

            items(rememberRates.value) { item ->
                ItemExchangeRate(item, currentCurrency = currentCurrency)
            }
        }
    }

}


@Composable
fun EmptyListRates() {
    Center {
        Column {
            Text(
                stringResource(id = R.string.error_rates),
                style = TextStyle(color = Color.Gray)
            )
        }
    }
}


@Composable
fun ShowErrorList() {
    Center {
        Column {
            Text(
                stringResource(id = R.string.error_rates),
                style = TextStyle(color = Color.Gray)
            )
        }
    }

}

@ExperimentalComposeUiApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShowListRates(rates: List<ExchangeRate>, currentCurrency: String) {
    when (rates.isNotEmpty()) {
        true -> ShowDataListRates(rates, currentCurrency)
        else -> EmptyListRates()
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(widthDp = 350, heightDp = 250)
@Composable
fun SimpleShowListRatesPreview() {
    ShowListRates(
        rates = arrayListOf(
            ExchangeRate(
                "TNDEUR",
                1000.0, 122.2, Date.from(Instant.EPOCH)
            ),
            ExchangeRate(
                "TNDJP",
                3.0, 4.2, Date.from(Instant.EPOCH)
            )
        ),
        currentCurrency = "TND"
    )
}

@Composable
fun SearchCurrencies(modifier: Modifier = Modifier) {
    var search: String by remember {
        mutableStateOf("")
    }
    var searchStated by remember {
        mutableStateOf(false)
    }

    val scopes = rememberCoroutineScope()
    val requesterFocus = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Row(modifier) {

        SpacerWidth(width = 12.dp)
        OutlinedTextField(
            value = search,
            onValueChange = { searchText ->
                search = searchText
            },
            Modifier
                .weight(0.65f)
                .fillMaxWidth(0.7f)
                .animateContentSize()
                .focusRequester(requesterFocus)
                .focusTarget()
                .onFocusChanged {
                    if (it.isFocused) {
                        searchStated = true
                    } else if (it.isCaptured) {
                        searchStated = false
                    }
                },
            textStyle = TextStyle(
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
            },
            label = null,
            placeholder = {
                Text(stringResource(id = R.string.search_label))
            },

            )
        SpacerWidth(width = 8.dp)
        AnimatedVisibility(visible = searchStated) {
            TextButton(
                onClick = {
                    requesterFocus.captureFocus()
                    requesterFocus.freeFocus()
                    focusManager.clearFocus()
                    searchStated = false
                    search = ""
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxHeight()
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = android.R.string.cancel),
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black,

                    )
            }
        }
    }

}