package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dali.hamza.core.common.ISessionManager
import dali.hamza.core.common.SessionManager
import dali.hamza.core.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CurrencyConvertViewModel(
  //  private val repository: CurrencyRepository,
    private val sessionManager: ISessionManager
) : ViewModel() {
    val currencyInSelect = mutableStateOf("")
    val currencyOutSelect = mutableStateOf("")

    init {
        viewModelScope.launch(Dispatchers.Main) {
            currencyInSelect.value = sessionManager.getCurrencyFromDataStore.first()
        }

    }


    fun selectCurrencyIn(currency: String) {
        currencyInSelect.value = currency
    }

    fun selectCurrencyOut(currency: String) {
        currencyOutSelect.value = currency
    }

    fun flipCurrencies() {
        val outCurrency = currencyOutSelect.value
        currencyOutSelect.value = currencyInSelect.value
        currencyInSelect.value = outCurrency
    }
}