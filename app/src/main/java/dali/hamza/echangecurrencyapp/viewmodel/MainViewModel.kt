package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dali.hamza.core.common.SessionManager
import dali.hamza.core.interactor.CalculateRatesUseCase
import dali.hamza.core.interactor.SaveOrUpdateRatesUseCase
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.initAmountInput
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val saveOrUpdateRatesUseCase: SaveOrUpdateRatesUseCase,
    private val calculateRatesUseCase: CalculateRatesUseCase
) : ViewModel() {

    private var selectedCurrency: String by mutableStateOf("")

    var isLoading: Boolean by mutableStateOf(false)

    private var mutableFlowExchangesRates: MutableStateFlow<IResponse?> = MutableStateFlow(null)
    private var exchangesRates: StateFlow<IResponse?> = mutableFlowExchangesRates


    fun getCurrencySelection() = selectedCurrency

    fun getExchangeRates(): StateFlow<IResponse?> = exchangesRates

    fun setCurrencySelection(newCurrency: String) {
        selectedCurrency = newCurrency
    }


    var mutableFlowAutoWalletForm: AmountInput by mutableStateOf(initAmountInput())
        private set


    fun changeAmount(amount: String) {
        mutableFlowAutoWalletForm = mutableFlowAutoWalletForm.copy(
            amount = amount
        )
    }

    fun retrieveOrUpdateRates(currency: String) {
        viewModelScope.launch {
            async {
                if (selectedCurrency.isNotEmpty() && selectedCurrency != currency) {
                    saveOrUpdateRatesUseCase.invoke()
                }
            }.await()
        }
    }


    fun calculateExchangeRates(amount: Double) {
        viewModelScope.launch {
            calculateRatesUseCase.invoke(amount).collect { response ->
                mutableFlowExchangesRates.value = response
            }
        }
    }


}