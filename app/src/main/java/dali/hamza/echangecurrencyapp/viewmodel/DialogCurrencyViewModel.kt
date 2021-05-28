package dali.hamza.echangecurrencyapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dali.hamza.core.common.SessionManager
import dali.hamza.core.interactor.GetCurrenciesListUseCase
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.echangecurrencyapp.models.CurrencyDTO
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialogCurrencyViewModel @Inject constructor(
    private val getCurrenciesListUseCase: GetCurrenciesListUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {


    private var currentCurrencyMutableFlow = retrieveCurrentCurrency()
    private val currentCurrencyFlow: StateFlow<String> by lazy {
        currentCurrencyMutableFlow
    }
    private var cacheList: MutableList<Currency> = emptyList<Currency>().toMutableList()

    private var currenciesMutableFlow: MutableStateFlow<IResponse?> = MutableStateFlow(null)
    private val currenciesFlow: StateFlow<IResponse?> by lazy {
        currenciesMutableFlow
    }

    fun getCurrencies() = currenciesFlow

    fun getCurrentCurrency() = currentCurrencyFlow
    fun setCacheList(cacheCurrencies: List<Currency>) {
        cacheList.clear()
        cacheList.addAll(cacheCurrencies)
    }

    private fun retrieveCurrentCurrency(): StateFlow<String> {
        val currency: MutableStateFlow<String> = MutableStateFlow("")
        viewModelScope.launch(IO) {

        }
        return currency
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
                MyResponse.SuccessResponse(listSearchableCurrency.toSet().toList())

        }
    }

    fun getCurrenciesFromLocalDb() {
        viewModelScope.launch(IO) {
            getCurrenciesListUseCase.invoke().collect {
                currenciesMutableFlow.value = it
            }
        }
    }

    fun setPreferenceCurrency(currencySelected: Currency) {
        viewModelScope.launch(IO) {
            sessionManager.setCurrencySelected(currency = currencySelected.name)
        }
    }


}