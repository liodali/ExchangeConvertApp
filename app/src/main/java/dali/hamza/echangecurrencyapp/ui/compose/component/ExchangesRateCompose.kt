package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.coil.rememberCoilPainter
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.ui.MainActivity
import okhttp3.internal.format
import kotlin.math.withSign

@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExchangesRatesGrid() {
    val viewModel = MainActivity.mainViewModelComposition.current
    val ratesResponse by viewModel.getExchangeRates().collectAsState()

    val amount = viewModel.mutableFlowAutoWalletForm.amount
    val loading = viewModel.isLoading

    when (ratesResponse) {
        is MyResponse.ErrorResponse<*> -> {
            Text("error")
        }
        is MyResponse.SuccessResponse<*> -> {
            val rates = (ratesResponse as MyResponse.SuccessResponse<*>).data as List<ExchangeRate>
            viewModel.isLoading = false
            LazyVerticalGrid(
                cells = GridCells.Fixed(2),
            ) {
                items(rates.size) { index ->
                    Card(
                        modifier = Modifier
                            .padding(2.dp)
                            .padding(8.dp),
                        elevation = 5.dp,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        ListItem(
                            text = {
                                Text(
                                    rates[index].name,
                                    fontSize = 15.sp,
                                )
                            },
                            secondaryText = {
                                Text(
                                    text = format("%.2f", rates[index].calculedAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(align = Alignment.End)
                                )
                            }
                        )
                    }
                }
            }
        }
        else -> {
            if (loading)
                Loading()
            Box() {

            }
        }
    }


}

@Composable
fun ItemExchangeRate() {
    Image(
        painter = rememberCoilPainter("https://www.countryflags.io/be/flat/32.png"),
        modifier = Modifier
            .width(56.dp)
            .height(48.dp),
        contentDescription = "",
    )
}