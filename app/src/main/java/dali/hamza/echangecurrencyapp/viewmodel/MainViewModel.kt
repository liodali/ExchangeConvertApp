package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dali.hamza.core.common.SessionManager
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.models.IResponse
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.initAmountInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private var selectedCurrency: MutableState<String> = mutableStateOf("")

    var isLoading: Boolean by mutableStateOf(false)

    private var mutableFlowExchangesRates: MutableStateFlow<IResponse?> = MutableStateFlow(null)
    private var stateFlowExchangesRates: StateFlow<IResponse?> = mutableFlowExchangesRates

    init {
        loadingCurrentCurrency()
    }

    fun getCurrencySelection() = selectedCurrency

    fun getExchangeRates(): StateFlow<IResponse?> = stateFlowExchangesRates

    fun setCurrencySelection(newCurrency: String) {
        selectedCurrency.value = newCurrency
    }


    private var cacheAmount: String? by mutableStateOf(null)
    var mutableFlowAmountForm: AmountInput by mutableStateOf(initAmountInput())
        private set


    fun changeAmount(amount: String) {
        mutableFlowAmountForm = mutableFlowAmountForm.copy(
            amount = amount
        )
    }

    fun loadingCurrentCurrency() {
        viewModelScope.launch(Dispatchers.Main) {
            sessionManager.getCurrencyFromDataStore.collect { currency ->
                if (selectedCurrency.value != currency
                    && selectedCurrency.value.isNotEmpty()
                ) {
                    withContext(Dispatchers.IO) {
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
                repository.saveExchangeRatesOfCurrentCurrency()
            }.await()
        }
    }

    fun resetExchangeRates() {
        cacheAmount = null
        mutableFlowExchangesRates.value = null
    }

    fun calculateExchangeRates(amount: Double) {
        if (amount != 0.0) {
            viewModelScope.launch {
                cacheAmount = amount.toString()
                isLoading = true
                mutableFlowExchangesRates.value = null
                retrieveOrUpdateRates()
                repository.getListRatesCurrencies(amount).collect { response ->
                    mutableFlowExchangesRates.value = response
                    isLoading = false
                }

            }
        }
    }


}