package dali.hamza.echangecurrencyapp.ui.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.compose.component.EmptyData
import dali.hamza.echangecurrencyapp.ui.compose.component.Loading
import dali.hamza.echangecurrencyapp.viewmodel.DialogCurrencyViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun BottomSheetCurrencies(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewModel = koinViewModel<DialogCurrencyViewModel>()
    Card(
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .then(modifier)
    ) {
        Column {
            TopHeaderBottomSheetCurrencies(
                selectedCurrency = viewModel.getCurrentCurrency().collectAsState().value,
                onClose = onClose,
                onSelect = { selection ->
                    scope.launch {
                        viewModel.setPreferenceCurrency(selection)
                    }
                    viewModel.mutableFlowSearchCurrency = ""
                    onSelect(selection)
                }
            )
            BodyCurrenciesBottomSheet()
        }
    }
}

@Composable
fun TopHeaderBottomSheetCurrencies(
    modifier: Modifier = Modifier,
    selectedCurrency: String,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .then(modifier)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.weight(.4f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "close BottomSheet")
        }
        Text(
            text = stringResource(id = R.string.selectTitleCurrency),
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(start = 6.dp, top = 8.dp, end = 6.dp)
        )

        Button(
            onClick = {
                onSelect(selectedCurrency)
            },
            shape = RoundedCornerShape(8.dp),
            enabled = selectedCurrency.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 3.dp)
        ) {
            Text(
                text = stringResource(id = R.string.selectLabel),
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BodyCurrenciesBottomSheet(modifier: Modifier = Modifier) {
    val vm = koinViewModel<DialogCurrencyViewModel>()
    val response = koinViewModel<DialogCurrencyViewModel>().getCurrencies().collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val loading = vm.isLoadingState
    LaunchedEffect(key1 = vm) {
        coroutineScope.launch {
            vm.getCurrenciesFromLocalDb()
        }
    }
    when (loading) {
        true -> {
            Loading()
        }

        else -> {
            if (response.value is MyResponse.SuccessResponse<*>) {
                val responseData =
                    (response.value as MyResponse.SuccessResponse<*>).data as List<Currency>
                var listCurrencies by rememberSaveable {
                    mutableStateOf(responseData)
                }
                var scrollTo by remember {
                    mutableStateOf(0)
                }
                LaunchedEffect(key1 = responseData) {
                    val currentCurrencySelected = vm.getCurrentCurrency().value
                    val index = listCurrencies.map { currency -> currency.name }
                        .indexOf(currentCurrencySelected)
                    if (index > 10) {
                        scrollTo = index - 3
                    } else {
                        scrollTo = index
                    }
                }
                LaunchedEffect(key1 = vm.mutableFlowSearchCurrency) {
                    if (vm.mutableFlowSearchCurrency.isEmpty()) {
                        listCurrencies = responseData
                    } else {
                        listCurrencies = responseData.filter { currency ->
                            currency.name.lowercase()
                                .contains(vm.mutableFlowSearchCurrency.lowercase())
                        }
                    }
                }
                Column(modifier = Modifier.background(color = Color.White)) {
                    SearchTextFieldCurrency(
                        text = vm.mutableFlowSearchCurrency,
                        onChange = { searchableText ->
                            vm.mutableFlowSearchCurrency = searchableText
                        })
                    ListCurrenciesBottomSheet(
                        currencies = listCurrencies,
                        selected = vm.getCurrentCurrency().collectAsState().value,
                        onChange = { name ->
                            vm.setSelectedCurrency(name)
                        },
                        scrollToIndex = scrollTo
                    )
                }
            } else {
                EmptyData(text = "No currencies available")
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchTextFieldCurrency(
    modifier: Modifier = Modifier,
    text: String,
    onChange: (String) -> Unit,
    trailingIcon: @Composable() (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        text,
        onChange,
        label = {
            Text(stringResource(id = R.string.search_label))
        },
        placeholder = {
            Text(stringResource(id = R.string.search_label))
        },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, "")
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        colors = TextFieldDefaults.colors(

        ),
        shape = RoundedCornerShape(2.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()

            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .then(modifier),

        )
}

@Composable
fun ListCurrenciesBottomSheet(
    modifier: Modifier = Modifier,
    currencies: List<Currency>,
    selected: String,
    onChange: (String) -> Unit,
    scrollToIndex: Int = 0
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = scrollToIndex) {
        scope.launch {
            if (scrollToIndex > 0) {
                listState.animateScrollToItem(scrollToIndex)
            }
        }
    }
    DisposableEffect(key1 = currencies) {
        onDispose {
            scope.cancel()
        }
    }
    LazyColumn(modifier = modifier, state = listState) {
        items(count = currencies.size) { index ->
            ItemListCurrenciesBottomSheet(
                currency = currencies[index],
                selected = selected,
                onChange = onChange
            )
        }
    }
}

@Composable
fun ItemListCurrenciesBottomSheet(
    modifier: Modifier = Modifier,
    currency: Currency,
    selected: String?,
    onChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 8.dp)
            .then(modifier)
    ) {
        Text(text = buildString {
            append(currency.fullCountryName)
            append(" (")
            append(currency.name)
            append(") ")
        }, modifier = Modifier.weight(2f))
        Checkbox(
            checked = currency.name.lowercase(Locale.ROOT) == selected?.lowercase(Locale.ROOT),
            onCheckedChange = { isSelected ->
                if (isSelected) {
                    onChange(currency.name)
                }
            })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(widthDp = 350, heightDp = 100)
@Composable
fun ShowTopBottomSheetPreview() {
    TopHeaderBottomSheetCurrencies(selectedCurrency = "", onClose = {}, onSelect = {})

}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(widthDp = 350, heightDp = 150)
@Composable
fun ShowSearchCurrencyPreview() {
    SearchTextFieldCurrency(text = "", onChange = {

    })
}

@Preview(widthDp = 350, heightDp = 100)
@Composable
fun ShowItemListCurrenciesPreview() {
    ItemListCurrenciesBottomSheet(
        modifier = Modifier
            .padding(start = 6.dp)
            .background(color = Color.White),
        currency = Currency(name = "EUR", fullCountryName = "Europe"),
        selected = "EUR",
        onChange = {

        })
}

@Preview(widthDp = 350, heightDp = 100)
@Composable
fun ShowItemListCurrencies2Preview() {
    ItemListCurrenciesBottomSheet(
        modifier = Modifier
            .padding(start = 6.dp)
            .background(color = Color.White),
        currency = Currency(name = "EUR", fullCountryName = "Europe"),
        selected = "TND",
        onChange = {

        })
}

@Preview(widthDp = 350, heightDp = 150)
@Composable
fun ShowListCurrenciesPreview() {
    ListCurrenciesBottomSheet(
        modifier = Modifier.background(color = Color.White),
        currencies = arrayListOf(
            Currency(name = "EUR", fullCountryName = "Europe"),
            Currency(name = "USD", fullCountryName = "US"),
            Currency(name = "JP", fullCountryName = "Japon"),
        ), selected = "EUR", onChange = {

        }

    )
}