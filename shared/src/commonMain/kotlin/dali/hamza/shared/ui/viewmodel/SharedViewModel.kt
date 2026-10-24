package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.currentLanguageTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * UI state shared by the Converter and Rates screens.
 */
data class SharedUiState(
    val username: String = "SOVEREIGN",
    val currencies: List<Currency> = emptyList(),
    val rates: List<ExchangeRate> = emptyList(),
    /** ISO code the cached [rates] table is quoted against (the user's
     *  market base — converter From is independent of it). */
    val ratesBase: String = "",
    /** Persisted dashboard preference, independent of the converter pair. */
    val marketBase: String = "",
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
    private val ratesMutex = Mutex()

    private val _state = MutableStateFlow(SharedUiState())
    val state: StateFlow<SharedUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val base = currentBase()
            _state.update { it.copy(username = repository.getUsername(), marketBase = base) }
            loadCurrencies()
            loadRates()
            // localized currency names follow the active language: sync in
            // the background (backend CLDR via ?lang=) and refresh on change
            viewModelScope.launch {
                runCatching { repository.syncCurrencyNames(currentLanguageTag()) }
                    .getOrNull()
                    ?.takeIf { it }
                    ?.let { loadCurrencies() }
            }
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

    private suspend fun loadRates() = ratesMutex.withLock { loadRatesLocked() }

    private suspend fun loadRatesLocked() {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val response = repository.getListRatesCurrencies(1.0)) {
            is MyResponse.Success -> {
                // The rates table is quoted against the STORED market base —
                // capture it so conversions can derive cross rates without
                // ever touching the user's market base.
                val base = currentBase()
                _state.update { current ->
                    current.copy(
                        rates = response.data,
                        isLoading = false,
                        ratesBase = base,
                        fromCurrency = current.fromCurrency
                            ?: CurrenciesCatalog.byCode(base),
                    )
                }
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

    /**
     * Converter-local From selection — deliberately does NOT touch the
     * stored market base: conversions use cross rates from the cached
     * base-quoted table (see [crossRate]). Changing From in the converter
     * must never rebase Market Overview.
     */
    fun selectFromCurrency(currency: Currency) {
        _state.update { it.copy(fromCurrency = currency, convertedAmount = null) }
        recomputeConversion()
    }

    /** Account/onboarding action: persist the dashboard base and reload its rates. */
    fun selectMarketBase(code: String) {
        viewModelScope.launch {
            ratesMutex.withLock {
                repository.setCurrentCurrency(code)
                _state.update {
                    it.copy(marketBase = code, ratesBase = "", rates = emptyList(), convertedAmount = null)
                }
                loadRatesLocked()
            }
        }
    }

    fun selectToCurrency(currency: Currency) {
        _state.update { it.copy(toCurrency = currency) }
        recomputeConversion()
    }

    fun swapCurrencies() {
        val current = _state.value
        val newFrom = current.toCurrency ?: return
        _state.update {
            it.copy(
                fromCurrency = newFrom,
                toCurrency = current.fromCurrency,
                convertedAmount = null,
            )
        }
        recomputeConversion()
    }

    fun convert() {
        viewModelScope.launch {
            loadRates()
            // Phase 4: record every successful exchange in the ledger
            // (the only permitted SharedViewModel data hook — see plan §3.3)
            val current = _state.value
            val amount = current.amount.replace(',', '.').toDoubleOrNull()
            val from = current.fromCurrency?.name
            val to = current.toCurrency?.name
            val converted = current.convertedAmount
            if (amount != null && amount > 0.0 && from != null && to != null &&
                from != to && converted != null && converted > 0.0
            ) {
                repository.recordTransaction(
                    Transaction(
                        base = from,
                        quote = to,
                        amountBase = amount,
                        amountQuote = converted,
                        rate = converted / amount,
                        timestamp = nowMillis(),
                    )
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            ratesMutex.withLock {
                repository.refreshExchangeRates()
                loadRatesLocked()
            }
        }
    }

    /**
     * Rate of `from → to` for 1 unit, derived from the cached base-quoted
     * table: direct when from IS the base, inverse when to is, crossed
     * (to/from) otherwise. Null when the pair isn't covered yet.
     */
    fun currentRate(): Double? {
        val current = _state.value
        val from = current.fromCurrency?.name ?: return null
        val to = current.toCurrency?.name ?: return null
        return crossRate(from, to, current)
    }

    private fun crossRate(from: String, to: String, state: SharedUiState): Double? {
        if (from == to) return 1.0
        val toRate = state.rates.firstOrNull { it.name == to }?.rate
        val fromRate = state.rates.firstOrNull { it.name == from }?.rate
        val base = state.ratesBase
        return when {
            base.isNotEmpty() && from == base -> toRate
            base.isNotEmpty() && to == base ->
                fromRate?.takeIf { it != 0.0 }?.let { 1.0 / it }
            toRate != null && fromRate != null && fromRate != 0.0 -> toRate / fromRate
            else -> null
        }
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
                else -> crossRate(from, to, current)?.let { amountValue * it }
            }
            current.copy(convertedAmount = converted)
        }
    }

    private suspend fun currentBase(): String = repository.getCurrentCurrency()
}
