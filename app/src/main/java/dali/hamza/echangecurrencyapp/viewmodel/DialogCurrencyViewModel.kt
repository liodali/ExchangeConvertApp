package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dali.hamza.core.common.ISessionManager
import dali.hamza.core.common.SessionManager
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.LoadingUIState
import dali.hamza.echangecurrencyapp.models.UIState
import dali.hamza.echangecurrencyapp.models.toUIState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class DialogCurrencyViewModel(
    private val repository: CurrencyRepository,
    private val sessionManager: ISessionManager
) : ViewModel() {


    var mutableFlowSearchCurrency: String by mutableStateOf("")

    private var currentCurrencyMutableFlow = retrieveCurrentCurrency()
    private val currentCurrencyFlow: StateFlow<String> by lazy {
        currentCurrencyMutableFlow
    }
    private var cacheList: MutableList<Currency> = emptyList<Currency>().toMutableList()

    private var currenciesMutableFlow: MutableStateFlow<UIState> =
        MutableStateFlow(LoadingUIState())
    private val currenciesFlow: StateFlow<UIState> by lazy {
        currenciesMutableFlow
    }


    fun getCurrencies() = currenciesFlow


    fun getCurrentCurrency() = currentCurrencyFlow
    fun setCacheList(cacheCurrencies: List<Currency>) {
        cacheList.clear()
        cacheList.addAll(cacheCurrencies)
    }

    private fun retrieveCurrentCurrency(): MutableStateFlow<String> {
        val currentCurrency: MutableStateFlow<String> = MutableStateFlow("")
        viewModelScope.launch(IO) {
            sessionManager.getCurrencyFromDataStore.collectLatest { currency ->
                currentCurrency.value = currency
            }
        }
        return currentCurrency
    }

    fun searchCurrencies(searchText: String) {
        viewModelScope.launch(IO) {
            val listSearchableCurrency = emptyList<Currency>().toMutableList()
            val texts = searchText.split(" ")
            texts.asSequence().asFlow().collect { text ->
                val lists = cacheList.filter {
                    it.name.contains(text, ignoreCase = true)
                }.toList()
                listSearchableCurrency.addAll(lists)
            }
            currenciesMutableFlow.value =
                MyResponse.SuccessResponse(listSearchableCurrency.toSet().toList()).toUIState()
        }
    }

    fun getCurrenciesFromLocalDb() {
        currenciesMutableFlow.value = LoadingUIState()
        viewModelScope.launch(IO) {
            val response = repository.getListCurrencies()
            currenciesMutableFlow.value = response.toUIState()
        }
    }

    fun setPreferenceCurrency(currencySelected: String) {
        viewModelScope.launch(IO) {
            sessionManager.setCurrencySelected(currency = currencySelected)
        }
    }

    fun setSelectedCurrency(currencySelected: String) {
        currentCurrencyMutableFlow.value = currencySelected
    }


}