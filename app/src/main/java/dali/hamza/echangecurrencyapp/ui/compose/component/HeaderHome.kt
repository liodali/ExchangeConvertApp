package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.MainActivity
import okhttp3.internal.format

@Composable
fun HeaderHomeCompose(
    openFragment: () -> Unit
) {
    val viewModel = MainActivity.mainViewModelComposition.current

    Column(
        modifier = Modifier.animateContentSize()
    ) {
        Crossfade(targetState = viewModel.showFormAmount, label = "") { isShow ->
            when (!isShow) {
                true -> {
                    Box(
                        modifier = Modifier
                            .height(156.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AmountConvertedCompose(
                            amount = viewModel.mutableFlowAmountForm.amount,
                            currency = viewModel.getCurrencySelection()!!
                        )
                    }
                }
                false -> {
                    InputAmount(
                        openCurrencyDialog = openFragment,
                        actionCalculate =
                        if (isShow && (viewModel.getCurrencySelection()?.isNotEmpty() == true)) {
                            {
                                val amount = viewModel.mutableFlowAmountForm.amount
                                if (amount.isNotEmpty() && amount.toDouble() > 0.0) {
                                    viewModel.isLoading = true
                                    viewModel.showFormAmount = false
                                    viewModel.calculateExchangeRates(viewModel.mutableFlowAmountForm.amount.toDouble())
                                }

                            }
                        } else null,
                        {
                            viewModel.changeAmount(it)
                        },
                        form = viewModel.mutableFlowAmountForm
                    )
                }
            }
        }
    }
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
                    append(format("%.2f",amount.toDouble()))
                }
            }
        )

    }
}