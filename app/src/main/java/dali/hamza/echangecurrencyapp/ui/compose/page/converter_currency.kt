package dali.hamza.echangecurrencyapp.ui.compose.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dali.hamza.core.common.ISessionManager
import dali.hamza.echangecurrencyapp.R
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.amountInputSaver
import dali.hamza.echangecurrencyapp.ui.compose.component.InputAmount
import dali.hamza.echangecurrencyapp.viewmodel.CurrencyConvertViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope
import java.util.Date

class ConverterCurrencyScope : KoinScopeComponent {
    override val scope: Scope by lazy { createScope(this) }
}

@Composable
fun ConverterCurrency(modifier: Modifier = Modifier) {

    Column(
        modifier = Modifier
            .then(modifier)
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        ConvertForm(openDialog = {

        })
    }
}

@Composable
fun ConvertForm(
    modifier: Modifier = Modifier,
    openDialog: () -> Unit,
    viewModel: CurrencyConvertViewModel = koinViewModel()
) {

    val currencyIn = viewModel.currencyInSelect.value
    val currencyOut = viewModel.currencyOutSelect.value
    val amountInSavable = rememberSaveable(stateSaver = amountInputSaver) {
        mutableStateOf(AmountInput("0.0"))
    }
    val amountOutSavable = rememberSaveable(stateSaver = amountInputSaver) {
        mutableStateOf(AmountInput("0.0"))
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        InputAmount(
            openCurrencyDialog = { openDialog() },
            actionCalculate = {
                if (currencyIn.isNotEmpty() && currencyOut.isNotEmpty()) {
                    //
                }
            },
            onValueChanged = {
                amountInSavable.value = AmountInput(it)
            },
            form = AmountInput(""),
            currency = currencyIn,
            suffixIcon = when {
                currencyIn.isNotEmpty() -> null
                else -> {
                    {
                        IconButton(
                            onClick = { }, modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.coin),
                                contentDescription = "select currency",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        IconButton(
            onClick = { viewModel.flipCurrencies() }, modifier = Modifier.align(Alignment.End)
        ) {
            Icon(imageVector = Icons.Filled.SwapVert, contentDescription = "switch currencies")
        }
        InputAmount(
            openCurrencyDialog = { openDialog() },
            actionCalculate = { /*TODO*/ },
            onValueChanged = {
                amountOutSavable.value = AmountInput(it)
            },
            form = AmountInput(""),
            currency = currencyOut,
            suffixIcon = when {
                currencyOut.isNotEmpty() -> null
                else -> {
                    {
                        IconButton(
                            onClick = { }, modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.coin),
                                contentDescription = "select currency",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@OptIn(KoinExperimentalAPI::class)
@Preview
@Composable
fun FormConverterPreview() {
    ConvertForm(
        modifier = Modifier.background(color = Color.White),
        openDialog = {},
        viewModel = CurrencyConvertViewModel(sessionManager = object : ISessionManager {
            override val getLastUTimeUpdateRates: Flow<Date>
                get() = TODO("Not yet implemented")
            override val getCurrencyFromDataStore: Flow<String> = flow {
                emit("EUR")
            }


            override suspend fun setCurrencySelected(currency: String) {
                TODO("Not yet implemented")
            }

            override suspend fun setTimeNowLastUpdateRate() {
                TODO("Not yet implemented")
            }

            override suspend fun setTimeLastUpdateRate(time: Long) {
                TODO("Not yet implemented")
            }

            override suspend fun removeTimeLastUpdateRate() {
                TODO("Not yet implemented")
            }

            override suspend fun clear() {
                TODO("Not yet implemented")
            }

        })
    )
}