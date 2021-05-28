package dali.hamza.echangecurrencyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.models.initAmountInput


class MainViewModel : ViewModel() {

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

}