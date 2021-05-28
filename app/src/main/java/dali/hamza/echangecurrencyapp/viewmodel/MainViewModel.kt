package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dali.hamza.core.common.SessionManager
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.initAmountInput
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel  @Inject constructor(
    private val sessionManager: SessionManager
): ViewModel() {

    private var selectedCurrency: String by mutableStateOf("")

    fun getCurrencySelection() = selectedCurrency

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
    fun retrieveCurrencySelection(){
        viewModelScope.launch {
            sessionManager.getCurrencyFromDataStore.collect {
                selectedCurrency = it
            }
        }
    }


}