@file:OptIn(ExperimentalCoroutinesApi::class)

package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dali.hamza.core.common.SessionManager
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.LoadingUIState
import dali.hamza.echangecurrencyapp.models.NoDataUIState
import dali.hamza.echangecurrencyapp.models.UIState
import dali.hamza.echangecurrencyapp.models.initAmountInput
import dali.hamza.echangecurrencyapp.models.toUIState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class State {
    NOT_LOADING, LOADING, FINISH
}

class MainViewModel(
    private val repository: CurrencyRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    // var isLoadingCurrentCurrency: State by mutableStateOf(State.NOT_LOADING)
    var showFormAmount: Boolean by mutableStateOf(true)
    var mutableStateAmountForm: AmountInput by mutableStateOf(initAmountInput())

    private var selectedCurrency: MutableState<String> = mutableStateOf("")


    private var mutableFlowAmountForm: MutableStateFlow<AmountInput> =
        MutableStateFlow(initAmountInput())
    private var stateFlowExchangesRates: StateFlow<UIState> =
        mutableFlowAmountForm.transformLatest { amountInput ->
            val rate = amountInput.amount.toDoubleOrNull()
            emit(LoadingUIState())
            when {
                rate == null || rate == 0.0 -> {
                    emit(MyResponse.NoResponse<Any>().toUIState())
                }

                else -> {
                    viewModelScope.launch {
                        val response = repository.getListRatesCurrencies(rate)
                        emit(response.toUIState())
                    }
                }
            }

        }/*.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)*/.stateIn(
            scope = viewModelScope,
            initialValue = NoDataUIState(),
            started = SharingStarted.WhileSubscribed(),
        )


    init {
        loadingCurrentCurrency()
    }

    fun getCurrencySelection() = selectedCurrency
    fun hasCurrencySelection() = selectedCurrency.value.isNotEmpty()

    fun getExchangeRates(): StateFlow<UIState> = stateFlowExchangesRates

    fun setCurrencySelection(newCurrency: String) {
        selectedCurrency.value = newCurrency
    }


    private var cacheAmount: String? by mutableStateOf(null)


    fun changeAmount(amount: String) {
        mutableStateAmountForm = mutableStateAmountForm.copy(
            amount = amount
        )
    }


    private fun loadingCurrentCurrency() {
        viewModelScope.launch(Main) {
            sessionManager.getCurrencyFromDataStore.collect { currency ->
                if (selectedCurrency.value != currency
                    && selectedCurrency.value.isNotEmpty()
                ) {
                    withContext(IO) {
                        sessionManager.removeTimeLastUpdateRate()
                        retrieveOrUpdateRates()
                    }
                }
                setCurrencySelection(currency)
            }
        }
    }

    private fun retrieveOrUpdateRates() {
        viewModelScope.launch {
            async {
                repository.saveExchangeRatesOfCurrentCurrency(IO)
            }.await()
        }
    }


    fun calculateExchangeRates(amount: Double) {
        if (amount != 0.0) {
            viewModelScope.launch {

                cacheAmount = amount.toString()

                mutableFlowAmountForm.value = mutableStateAmountForm
            }
        }
    }

}

/*class MyViewModelLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}*/