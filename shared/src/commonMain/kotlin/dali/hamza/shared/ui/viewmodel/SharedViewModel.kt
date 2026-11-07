package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.common.MetalPricing
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
    val username: String = dali.hamza.shared.data.storage.DEFAULT_USERNAME,
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
    /**
     * Mass unit of [amount] when From is a metal (XAU/XAG): the user types
     * grams/ounces/kilos, conversions convert to troy ounces internally.
     * Null for fiat — the amount is then plain currency units.
     */
    val fromUnit: dali.hamza.shared.common.MetalPricing.GoldUnit? = null,
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

    /**
     * Push a display-name change into the UI state live — Account's Edit
     * Profile persists via its own ViewModel; without this the Home top
     * bar keeps the name captured at [load] until relaunch.
     */
    fun onUsernameChanged(name: String) {
        _state.update { it.copy(username = name) }
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
     *
     * Metals default their amount unit to grams; fiat clears it.
     */
    fun selectFromCurrency(currency: Currency) {
        _state.update {
            it.copy(
                fromCurrency = currency,
                convertedAmount = null,
                fromUnit = if (currency.name in CurrenciesCatalog.metalCodes) {
                    MetalPricing.GoldUnit.GRAM
                } else {
                    null
                },
            )
        }
        recomputeConversion()
    }

    /** Mass-unit suffix of the amount input (metals only; g → oz → kg). */
    fun selectFromUnit(unit: MetalPricing.GoldUnit) {
        _state.update { it.copy(fromUnit = unit) }
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
                // To is fiat-only, so the new From never carries a mass unit
                fromUnit = null,
            )
        }
        recomputeConversion()
    }

    fun convert() {
        viewModelScope.launch {
            loadRates()
            // assets (XAU…) aren't in the base table: settle the on-demand
            // pair rate BEFORE reading the result, so Execute records the
            // real conversion instead of skipping a still-pending one
            _state.value.let { state ->
                val from = state.fromCurrency?.name
                val to = state.toCurrency?.name
                if (from != null && to != null && from != to && pairNeedsFetch(state)) {
                    ensurePairRate(from, to)
                }
            }
            // Phase 4: record every successful exchange in the ledger
            // (the only permitted SharedViewModel data hook — see plan §3.3)
            val current = _state.value
            val amountText = current.amount.replace(',', '.').toDoubleOrNull()
            val from = current.fromCurrency?.name
            val to = current.toCurrency?.name
            // ledger amounts are in base-currency units: troy ounces for
            // metals (the unit the rate is quoted against)
            val amount = amountText?.let { amount ->
                current.fromUnit?.let { MetalPricing.toTroyOunces(amount, it) } ?: amount
            }
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
                // pair quotes go stale with the table — re-quote metals on
                // the next conversion instead of serving the old cache
                pairRates.clear()
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
        val tableRate = when {
            base.isNotEmpty() && from == base -> toRate
            base.isNotEmpty() && to == base ->
                fromRate?.takeIf { it != 0.0 }?.let { 1.0 / it }
            toRate != null && fromRate != null && fromRate != 0.0 -> toRate / fromRate
            else -> null
        }
        // assets (XAU, XAG, BTC) never ride in the base-quoted table —
        // their pairs resolve from the on-demand pair cache instead
        return tableRate ?: cachedPairRate(from, to)
    }

    /**
     * On-demand pair rates for legs missing from the base table:
     * "FROM:TO" → (rate, fetchedAtMs). Metals need the symbol-filtered
     * backend call (plans/gold-section.md all-fiat pivot); the cache keeps
     * keystroke recomputes offline after the first fetch.
     */
    private val pairRates = mutableMapOf<String, Pair<Double, Long>>()
    private var pairFetchJob: kotlinx.coroutines.Job? = null
    private var pairFetchKey: String? = null

    private fun cachedPairRate(from: String, to: String): Double? {
        val now = nowMillis()
        pairRates["$from:$to"]?.let { (rate, at) ->
            if (now - at < PAIR_RATE_TTL_MS) return rate
        }
        pairRates["$to:$from"]?.let { (rate, at) ->
            if (now - at < PAIR_RATE_TTL_MS && rate != 0.0) return 1.0 / rate
        }
        return null
    }

    /** True when the active pair involves an asset the table can't quote. */
    private fun pairNeedsFetch(state: SharedUiState): Boolean {
        val from = state.fromCurrency?.name ?: return false
        val to = state.toCurrency?.name ?: return false
        if (from == to) return false
        if (crossRate(from, to, state) != null) return false
        // fiat-only pairs are always table-derivable (or the table is still
        // loading) — never fetch pair rates for them
        return CurrenciesCatalog.assetCodes.any { it == from || it == to }
    }

    /**
     * Fetch and cache the active pair's rate, then recompute — suspends so
     * [convert] can await a settled result before recording the ledger.
     */
    private suspend fun ensurePairRate(from: String, to: String) {
        when (val response = repository.getLiveRateForPair(from, to)) {
            is MyResponse.Success ->
                response.data
                    .takeIf { it > 0.0 && it.isFinite() }
                    ?.let { pairRates["$from:$to"] = it to nowMillis() }
            // quotable nowhere / transport failure: leave the cache empty —
            // the UI shows no result rather than a fabricated one
            is MyResponse.Empty, is MyResponse.Error -> Unit
        }
        recomputeNow()
    }

    private fun recomputeConversion() {
        recomputeNow()
        val state = _state.value
        if (!pairNeedsFetch(state)) return
        val key = "${state.fromCurrency?.name}:${state.toCurrency?.name}"
        if (pairFetchJob?.isActive == true && pairFetchKey == key) return
        pairFetchKey = key
        pairFetchJob?.cancel()
        val from = state.fromCurrency?.name ?: return
        val to = state.toCurrency?.name ?: return
        pairFetchJob = viewModelScope.launch { ensurePairRate(from, to) }
    }

    private fun recomputeNow() {
        _state.update { current ->
            val amountValue = current.amount.replace(',', '.').toDoubleOrNull()
            val from = current.fromCurrency?.name
            val to = current.toCurrency?.name
            val converted = when {
                amountValue == null || amountValue == 0.0 -> null
                from == null || to == null -> null
                from == to -> amountValue
                else -> {
                    // metal amounts arrive in the selected mass unit —
                    // convert to troy ounces before applying the rate
                    val amountInBase = current.fromUnit
                        ?.let { MetalPricing.toTroyOunces(amountValue, it) }
                        ?: amountValue
                    crossRate(from, to, current)?.let { amountInBase * it }
                }
            }
            current.copy(convertedAmount = converted)
        }
    }

    private suspend fun currentBase(): String = repository.getCurrentCurrency()

    private companion object {
        /** Pair quotes go stale like the table: refetch at most half-hourly. */
        const val PAIR_RATE_TTL_MS = 30L * 60L * 1000L
    }
}
