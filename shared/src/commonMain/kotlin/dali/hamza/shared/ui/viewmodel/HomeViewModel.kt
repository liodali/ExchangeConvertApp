package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.common.DateUtils
import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.domain.models.ExchangeRate
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

/** One Market-Overview pair card (design `RbQhR` — full-width card + sparkline). */
data class PairCardData(
    val quote: String,
    val rate: Double,
    /** 7-day change in percent (null while the series loads). */
    val deltaPercent: Double? = null,
    /** 7-day rate series for the sparkline. */
    val spark: List<Double> = emptyList(),
)

data class HomeUiState(
    val pairCards: List<PairCardData> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * Dashboard state (design frame `RbQhR` — Phase 5): top-pair cards with
 * 7-day sparklines/deltas and the recent-activity ledger rows. The Quick
 * Exchange card stays on the frozen [SharedViewModel] (plan §2.1).
 */
class HomeViewModel(
    private val repository: IRepository,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Reload pair cards (7-day series) for the session base + top rates. */
    fun load(base: String?, topRates: List<ExchangeRate>) {
        if (base == null || topRates.isEmpty()) return
        val symbols = topRates.take(3).map { it.name }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val cards = topRates.take(3).map { rate ->
                PairCardData(quote = rate.name, rate = rate.rate)
            }
            _state.update { it.copy(pairCards = cards) }

            val today = DateUtils.epochDayOf(nowMillis())
            val from = DateUtils.toIsoDate(today - 7)
            val to = DateUtils.toIsoDate(today)
            when (val response = repository.getHistoricalRates(base, symbols, from, to)) {
                is MyResponse.Success -> {
                    val enriched = cards.map { card ->
                        val series = response.data[card.quote].orEmpty()
                        val first = series.firstOrNull()?.rate
                        val last = series.lastOrNull()?.rate
                        val delta = if (first != null && last != null && first != 0.0) {
                            (last - first) / first * 100.0
                        } else {
                            null
                        }
                        card.copy(
                            deltaPercent = delta,
                            spark = series.map { it.rate },
                        )
                    }
                    _state.update { it.copy(pairCards = enriched, isLoading = false) }
                }

                else -> _state.update { it.copy(isLoading = false) } // cards stay live-rate only
            }
        }
    }

    /** Refresh the recent-activity rows (call after a conversion executes). */
    fun refreshTransactions() {
        viewModelScope.launch {
            val transactions = repository.getTransactions()
            _state.update { it.copy(recentTransactions = transactions.take(2)) }
        }
    }

    /** Initial recent-activity load. */
    fun loadTransactions() = refreshTransactions()
}
