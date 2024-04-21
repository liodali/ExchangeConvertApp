package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.domain.models.ExchangeRate
import okhttp3.internal.format
import java.util.Calendar

@Composable
fun ItemExchangeRate(item: ExchangeRate, currentCurrency: String) {
    Card(
        modifier = Modifier
            //.background(color=  MaterialTheme.colorScheme.primary)
            .padding(
                vertical = 6.dp,
                horizontal = 4.dp
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp)
                .padding(2.dp)
        ) {


            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    .padding(2.dp)
                    .wrapContentWidth(align = Alignment.End)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    CurrencyFlagImage(
                        currency = item.name.split(currentCurrency).last().lowercase(),
                        size = 18.dp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        item.name.split(currentCurrency).last(),
                        fontSize = 15.sp,
                        maxLines = 1,

                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.wrapContentWidth(align = Alignment.Start)
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()

                ) {

                    Text(
                        text = format("%.2f",item.calculatedAmount),
                        fontSize = when {
                            item.rate > 10000 -> 18.sp
                            else -> 22.sp
                        },
                        style = TextStyle(textAlign = TextAlign.End),
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                            .wrapContentHeight(align = Alignment.CenterVertically)

                    )
                    Text(
                        text = "x" + format("%.2f", item.rate),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(bottom = 1.dp)
                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                    )
                }

            }

        }

    }
}


@Preview(widthDp = 102)
@Composable
fun ItemExchangeRatePreview() {
    ItemExchangeRate(
        ExchangeRate(
            "TND", 10002.4,
            300.4, Calendar.getInstance().time
        ),
        "eur"
    )
}