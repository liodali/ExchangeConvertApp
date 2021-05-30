package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import okhttp3.internal.format

@ExperimentalMaterialApi
@Composable
fun ExchangesRatesGrid() {
    val viewModel = MainActivity.mainViewModelComposition.current
    val loading = viewModel.isLoading

    val ratesState = viewModel.getExchangeRates().collectAsState()
    val ratesResponse = ratesState.value
    if (ratesResponse == null) {
        EmptyBox()
    }
    when (ratesResponse) {
        is MyResponse.SuccessResponse<*> -> {
            viewModel.isLoading = false
            ShowDataListRates(
                ratesResponse.data as List<ExchangeRate>,
            )
        }
        is MyResponse.ErrorResponse<*> -> ShowErrorList()
    }

    if (loading)
        Loading()

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowDataListRates(rates: List<ExchangeRate>) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(2)
    ) {

        items(rates) { item ->
            Card(
                modifier = Modifier
                    .padding(2.dp)
                    .padding(8.dp),
                elevation = 5.dp,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .padding(5.dp)
                ) {
                    Text(
                        item.name,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = format("%.2f", item.calculedAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun ShowListRates(rates: List<ExchangeRate>, viewModel: MainViewModel) {
    when (rates.isNotEmpty()) {
        true -> ShowDataListRates(rates)
        else -> EmptyListRates()
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

