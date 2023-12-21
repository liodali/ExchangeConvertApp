package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.repository.IRepository
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.initAmountInput
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(
    private val repository: IRepository
) : ViewModel() {

    var showFormAmount: Boolean by mutableStateOf(true)
    private var selectedCurrency: String? by mutableStateOf(null)

    var isLoading: Boolean by mutableStateOf(false)

    private var mutableFlowExchangesRates: MutableStateFlow<IResponse?> = MutableStateFlow(null)
    private var stateFlowExchangesRates: StateFlow<IResponse?> = mutableFlowExchangesRates


    fun getCurrencySelection() = selectedCurrency

    fun getExchangeRates(): StateFlow<IResponse?> = stateFlowExchangesRates

    fun setCurrencySelection(newCurrency: String) {
        selectedCurrency = newCurrency
    }


    private var cacheAmount: String? by mutableStateOf(null)
    var mutableFlowAmountForm: AmountInput by mutableStateOf(initAmountInput())
        private set


    fun changeAmount(amount: String) {
        mutableFlowAmountForm = mutableFlowAmountForm.copy(
            amount = amount
        )
    }

    fun retrieveOrUpdateRates(currency: String) {
        viewModelScope.launch {
            async {
                if (selectedCurrency != null
                    && selectedCurrency!!.isNotEmpty()
                    && selectedCurrency != currency
                ) {
                    repository.saveExchangeRatesOfCurrentCurrency()
                }
            }.await()
        }
    }

    fun resetExchangeRates() {
        cacheAmount = null
        mutableFlowExchangesRates.value = null
    }

    fun calculateExchangeRates(amount: Double) {
        if (cacheAmount == null || (cacheAmount != null &&
                    cacheAmount!!.isNotEmpty()
                    && cacheAmount!!.toDouble() != amount)
        ) {
            viewModelScope.launch {
                cacheAmount = amount.toString()
                isLoading = true
                mutableFlowExchangesRates.value = null
                repository.getListRatesCurrencies(amount).collect { response ->
                    mutableFlowExchangesRates.value = response
                    isLoading = false
                }

            }
        }
    }


}