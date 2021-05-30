package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.ui.MainActivity

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HeaderHomeCompose(
    openFragment: () -> Unit
) {
    val viewModel = MainActivity.mainViewModelComposition.current

    Column(
        modifier = Modifier.animateContentSize()
    ) {
        Crossfade(targetState = viewModel.showFormAmount) { isShow ->

            when (!isShow) {
                true -> {
                    Box(
                        modifier = Modifier
                            .height(156.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            viewModel.mutableFlowAmountForm.amount,
                            fontSize = 45.sp
                        )

                    }
                }
                false -> {
                    InputAmount(
                        openCurrencyDialog = openFragment,
                        actionCalculate =
                        if (isShow) {
                            {
                                viewModel.isLoading = true
                                viewModel.showFormAmount = false
                                viewModel.calculateExchangeRates(viewModel.mutableFlowAmountForm.amount.toDouble())
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
        SpacerHeight(
            height = 8.dp
        )
        CurrencySelectionCompose(
            openFragment = openFragment
        )
    }
}