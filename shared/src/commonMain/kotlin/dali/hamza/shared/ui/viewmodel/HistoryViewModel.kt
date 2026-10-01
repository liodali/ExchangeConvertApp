package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Chart range chips (design `72Xe2`: daily backend data → 1W is default). */
enum class HistoryRange(val label: String, val days: Int?) {
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    ONE_YEAR("1Y", 365),
    ALL("ALL", null),
}

/** Net accumulated balance of one currency, computed from the ledger. */
data class AssetBalance(val currency: String, val balance: Double)

data class HistoryUiState(
    val base: String? = null,
    val quote: String? = null,
    val currentRate: Double? = null,
    val lastUpdatedLabel: String? = null,
    val range: HistoryRange = HistoryRange.ONE_WEEK,
    val series: List<HistoricalRate> = emptyList(),
    val changePercent: Double? = null,
    val delta: Double? = null,
    /** All-time best rate for the pair (stored ledger, series fallback). */
    val bestRate: Double? = null,
    val bestTimestamp: Long? = null,
    val transactions: List<Transaction> = emptyList(),
    val balances: List<AssetBalance> = emptyList(),
    val topPercentile: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * State holder for the History tab (design frame `72Xe2` — Asset
 * Performance & Records). KMP-safe, no platform ViewModel base.
 */
class HistoryViewModel(
    private val repository: IRepository,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    /** Load everything for the active pair (session base → selected quote). */
    fun load(base: String?, quote: String?) {
        if (base == null || quote == null || base == quote) {
            _state.update { it.copy(error = "Select two different currencies to see history") }
            return
        }
        _state.update { it.copy(base = base, quote = quote, error = null, isLoading = true) }
        viewModelScope.launch {
            refreshLiveRate(base, quote)
            loadTransactions(base, quote)
            loadSeries(base, quote, _state.value.range)
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun selectRange(range: HistoryRange) {
        val base = _state.value.base ?: return
        val quote = _state.value.quote ?: return
        _state.update { it.copy(range = range) }
        viewModelScope.launch { loadSeries(base, quote, range) }
    }

    private suspend fun refreshLiveRate(base: String, quote: String) {
        when (val response = repository.getListRatesCurrencies(1.0)) {
            is MyResponse.Success -> {
                val rate = response.data.firstOrNull { it.name == quote }?.rate
                if (rate != null) {
                    _state.update { it.copy(currentRate = rate) }
                }
            }

            else -> Unit
        }
    }

    private suspend fun loadSeries(
        base: String,
        quote: String,
        range: HistoryRange,
    ) {
        val today = DateUtils.epochDayOf(nowMillis())
        val fromEpochDay = range.days?.let { today - it } ?: (today - 365 * 5)
        val from = DateUtils.toIsoDate(fromEpochDay)
        val to = DateUtils.toIsoDate(today)
        when (val response = repository.getHistoricalRates(base, quote, from, to)) {
            is MyResponse.Success -> {
                val series = response.data
                val first = series.firstOrNull()?.rate
                val last = series.lastOrNull()?.rate
                val current = _state.value.currentRate ?: last
                val delta = if (first != null && last != null) last - first else null
                val changePercent = if (first != null && first != 0.0 && delta != null) {
                    delta / first * 100.0
                } else {
                    null
                }
                val topPercentile = current?.let { c ->
                    percentileRank(series.map { it.rate }, c)
                }
                _state.update {
                    it.copy(
                        series = series,
                        delta = delta,
                        changePercent = changePercent,
                        topPercentile = topPercentile,
                        lastUpdatedLabel = series.lastOrNull()?.date,
                        // series fallback when the ledger has no record yet
                        bestRate = it.bestRate ?: series.maxByOrNull { r -> r.rate }?.rate,
                        currentRate = current,
                    )
                }
            }

            is MyResponse.Error -> _state.update {
                it.copy(error = it.error ?: response.error.toString())
            }

            MyResponse.Empty -> Unit
        }
    }

    private suspend fun loadTransactions(base: String, quote: String) {
        val all = repository.getTransactions()
        val forPair = all.filter { it.base == base && it.quote == quote }
        val best = forPair.maxByOrNull { it.rate }
        _state.update {
            it.copy(
                transactions = forPair,
                balances = computeBalances(all),
                bestRate = best?.rate ?: it.bestRate,
                bestTimestamp = best?.timestamp,
            )
        }
    }

    companion object {
        /** Net per-currency balance: base is spent, quote is received. */
        fun computeBalances(transactions: List<Transaction>): List<AssetBalance> =
            transactions
                .fold(mutableMapOf<String, Double>()) { acc, tx ->
                    acc.apply {
                        put(tx.base, (get(tx.base) ?: 0.0) - tx.amountBase)
                        put(tx.quote, (get(tx.quote) ?: 0.0) + tx.amountQuote)
                    }
                }
                .map { AssetBalance(it.key, it.value) }
                .sortedByDescending { kotlin.math.abs(it.balance) }

        /** Percentage of values strictly below [value] (top-N% rank). */
        fun percentileRank(values: List<Double>, value: Double): Int? {
            if (values.isEmpty()) return null
            val below = values.count { it < value }
            return 100 - (below * 100 / values.size)
        }
    }
}
