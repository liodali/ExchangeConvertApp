package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.format

@ExperimentalComposeUiApi
@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun ExchangesRatesGrid() {
    val viewModel = MainActivity.mainViewModelComposition.current
    val loading = viewModel.isLoading

    val ratesState = viewModel.getExchangeRates().collectAsState()
    val ratesResponse = ratesState.value
    if (loading)
        Loading()
    when {
        ratesResponse == null ->
            EmptyBox()
        ratesResponse is MyResponse.SuccessResponse<*> -> {
            viewModel.isLoading = false
            ShowListRates(
                ratesResponse.data as List<ExchangeRate>,
            )
        }
        ratesResponse is MyResponse.ErrorResponse<*> -> ShowErrorList()
    }
}

@ExperimentalComposeUiApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShowListRates(rates: List<ExchangeRate>) {
    when (rates.isNotEmpty()) {
        true -> ShowDataListRates(rates)
        else -> EmptyListRates()
    }

}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowDataListRates(rates: List<ExchangeRate>) {
    var search: String by remember {
        mutableStateOf("")
    }
    var searchStated by remember {
        mutableStateOf(false)
    }
    val rememberRates = rememberSaveable {
        mutableStateOf(rates)
    }
    val scopes = rememberCoroutineScope()
    val requesterFocus = FocusRequester()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Card(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        elevation = if (isSystemInDarkTheme()) 0.dp else 5.dp
    ) {
        Column() {
            Row(Modifier.weight(0.15f)) {

                SpacerWidth(width = 12.dp)
                OutlinedTextField(
                    value = search,
                    onValueChange = { searchText ->
                        search = searchText
                        when (searchText.isNotEmpty() && Regex("[a-zA-Z]{1,3}").matches(searchText)) {
                            true -> scopes.launch(IO) {
                                val list = rates.filter {
                                    it.name.toLowerCase().contains(searchText.toLowerCase())
                                }
                                withContext(Main) {
                                    rememberRates.value = list
                                }
                            }
                            else -> {
                                rememberRates.value = rates
                            }
                        }
                    },
                    Modifier
                        .weight(0.65f)
                        .fillMaxWidth(0.7f)
                        .animateContentSize()
                        .focusRequester(requesterFocus)
                        .focusModifier()
                        .onFocusChanged {
                            if (it == FocusState.Active) {
                                searchStated = true
                            } else if (it == FocusState.Disabled || it == FocusState.Captured) {
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
                            rememberRates.value = rates
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
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
                Modifier.weight(0.85f)
            ) {

                items(rememberRates.value) { item ->
                    ItemExchangeRate(item)
                }
            }
        }
    }

}

@Composable
fun ItemExchangeRate(item: ExchangeRate) {
    Card(
        modifier = Modifier
            .padding(2.dp)
            .padding(8.dp),
        elevation = 5.dp,
        shape = RoundedCornerShape(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .padding(3.dp)
                .padding(8.dp)
        ) {
            Text(
                item.name,
                fontSize = 15.sp,
                modifier = Modifier.wrapContentWidth(align = Alignment.Start)
            )
            Column(
                modifier = Modifier
                    .padding(2.dp)
                    .padding(5.dp)
                    .wrapContentWidth(align = Alignment.End)
            ) {
                Text(
                    text = "x" + format("%.2f", item.rate),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(align = Alignment.End)
                        .wrapContentHeight(align = Alignment.Top)
                )
                Text(
                    text = format("%.2f", item.calculedAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(align = Alignment.End)
                        .wrapContentHeight(align = Alignment.Bottom)

                )
            }
        }

    }
}


@Composable
fun EmptyListRates() {
    Center() {
        Column() {
            Text(
                stringResource(id = R.string.error_rates),
                style = TextStyle(color = Color.Gray)
            )
        }
    }
}


@Composable
fun ShowErrorList() {
    Center() {
        Column() {
            Text(
                stringResource(id = R.string.error_rates),
                style = TextStyle(color = Color.Gray)
            )
        }
    }

}

