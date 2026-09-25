package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state shared by the Converter and Rates screens.
 */
data class SharedUiState(
    val currencies: List<Currency> = emptyList(),
    val rates: List<ExchangeRate> = emptyList(),
    val amount: String = "",
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
    val convertedAmount: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Single source of truth for the shared Compose Multiplatform UI
 * (state-holder with [StateFlow]; no platform ViewModel base class).
 */
class SharedViewModel(
    private val repository: IRepository,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(SharedUiState())
    val state: StateFlow<SharedUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loadCurrencies()
            loadRates()
        }
    }

    private suspend fun loadCurrencies() {
        _state.update { it.copy(isLoading = true) }
        when (val response = repository.getListCurrencies()) {
            is MyResponse.Success -> _state.update { current ->
                current.copy(
                    currencies = response.data,
                    fromCurrency = current.fromCurrency
                        ?: response.data.firstOrNull { it.name == currentBase() }
                        ?: CurrenciesCatalog.byCode("USD"),
                    toCurrency = current.toCurrency ?: CurrenciesCatalog.byCode("EUR"),
                )
            }

            is MyResponse.Error -> _state.update { current ->
                current.copy(isLoading = false, error = response.error.toString())
            }

            MyResponse.Empty -> _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadRates(amount: Double = 1.0) {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val response = repository.getListRatesCurrencies(amount)) {
            is MyResponse.Success -> _state.update { current ->
                current.copy(
                    rates = response.data,
                    isLoading = false,
                    fromCurrency = current.fromCurrency
                        ?: CurrenciesCatalog.byCode(currentBase()),
                )
            }

            is MyResponse.Error -> _state.update { current ->
                current.copy(
                    isLoading = false,
                    error = (response.error as? String) ?: response.error.toString()
                )
            }

            MyResponse.Empty -> _state.update { it.copy(isLoading = false) }
        }
        recomputeConversion()
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amount = amount, error = null) }
        recomputeConversion()
    }

    fun selectFromCurrency(currency: Currency) {
        viewModelScope.launch {
            repository.setCurrentCurrency(currency.name)
            _state.update { it.copy(fromCurrency = currency, convertedAmount = null) }
            loadRates()
        }
    }

    fun selectToCurrency(currency: Currency) {
        _state.update { it.copy(toCurrency = currency) }
        recomputeConversion()
    }

    fun swapCurrencies() {
        val current = _state.value
        val newFrom = current.toCurrency ?: return
        viewModelScope.launch {
            repository.setCurrentCurrency(newFrom.name)
            _state.update { it.copy(fromCurrency = newFrom, toCurrency = current.fromCurrency) }
            loadRates()
        }
    }

    fun convert() {
        viewModelScope.launch {
            loadRates()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshExchangeRates()
            loadRates()
        }
    }

    /**
     * Rate of `from → to` for 1 unit, taken from the cached live rates.
     */
    fun currentRate(): Double? {
        val current = _state.value
        val from = current.fromCurrency?.name ?: return null
        val to = current.toCurrency?.name ?: return null
        if (from == to) return 1.0
        return current.rates.firstOrNull { it.name == to }?.rate
    }

    private fun recomputeConversion() {
        _state.update { current ->
            val amountValue = current.amount.replace(',', '.').toDoubleOrNull()
            val from = current.fromCurrency?.name
            val to = current.toCurrency?.name
            val converted = when {
                amountValue == null || amountValue == 0.0 -> null
                from == null || to == null -> null
                from == to -> amountValue
                else -> current.rates.firstOrNull { it.name == to }
                    ?.let { amountValue * it.rate }
            }
            current.copy(convertedAmount = converted)
        }
    }

    private suspend fun currentBase(): String = repository.getCurrentCurrency()
}
