package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.coil.rememberCoilPainter

@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExchangesRatesGrid(rates: List<String>) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 156.dp)
    ) {
        items(rates.size) { index ->
            Card(
                modifier = Modifier
                    .padding(2.dp)
                    .padding(8.dp),
                elevation = 5.dp,
            ) {
                ListItem(
                    text = {
                        Row() {
                            ItemExchangeRate()
                            Text(
                                rates[index]
                            )
                        }
                    },
                    secondaryText = {
                        Text(
                            text = "3.21TND",
                            fontSize = 17.sp,
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