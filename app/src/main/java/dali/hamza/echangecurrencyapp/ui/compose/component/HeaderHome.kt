package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import okhttp3.internal.format
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HeaderHomeCompose(
    openFragment: () -> Unit
) {
    val viewModel = koinViewModel<MainViewModel>()
    InputAmount(
        openCurrencyDialog = openFragment,
        actionCalculate =
        if (viewModel.getCurrencySelection().value.isNotEmpty()) {
            {
                val amount = viewModel.mutableFlowAmountForm.amount
                if (amount.isNotEmpty() && amount.toDouble() > 0.0) {
                    viewModel.isLoading = true
                    viewModel.showFormAmount = false
                    viewModel.calculateExchangeRates(viewModel.mutableFlowAmountForm.amount.toDouble())
                }

            }
        } else null,
        onValueChanged = { amount ->
            viewModel.changeAmount(amount)
        },
        clearText = {
            viewModel.changeAmount("")
        },
        form = viewModel.mutableFlowAmountForm,
        currency = viewModel.getCurrencySelection().value,
        keyboardController = LocalSoftwareKeyboardController.current,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    )

}

@Composable
fun AmountConvertedCompose(
    amount: String,
    currency: String
) {
    val colorText = if (isSystemInDarkTheme()) Color.White else Color.Black
    Column(
//        Modifier
//            .fillMaxWidth()
//            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = colorText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(currency)
                }
                append(" ")
                withStyle(
                    style = MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                        color = colorText,
                        fontSize = 35.sp
                    )
                ) {
                    append(format("%.2f", amount.toDouble()))
                }
            }
        )

    }
}