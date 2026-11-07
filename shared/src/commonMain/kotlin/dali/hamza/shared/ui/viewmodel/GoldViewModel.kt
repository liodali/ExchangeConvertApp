package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.domain.models.GoldSnapshot
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gold section state (plans/gold-section.md §5.3).
 *
 * - [snapshot] is the last VALID quote; [stale] marks it as surviving a
 *   failed refresh, [unavailable] means nothing was ever fetched (retry).
 * - [usdFallback] is set when the display base has no gold quote (asset
 *   base or non-quotable fiat) and an explicitly labeled USD value is
 *   shown instead — never another asset, never a mislabeled currency.
 * - [loading] only covers the "no data yet" phase: refreshing on top of a
 *   valid snapshot keeps it visible (no flicker, no reflow).
 */
data class GoldUiState(
    val snapshot: GoldSnapshot? = null,
    val loading: Boolean = false,
    val stale: Boolean = false,
    val unavailable: Boolean = false,
    val usdFallback: Boolean = false,
    /** Display currency the last load targeted (labels the fallback note). */
    val requestedCurrency: String = "USD",
)

/**
 * State holder for the Home Gold section. Loads independently of the
 * market cards — a gold failure never blocks Home or clears fiat data.
 *
 * Orchestration rules (plan §5.3):
 * - loads are keyed by display currency: switching base cancels the
 *   in-flight request and outdated responses are discarded (generation);
 * - duplicate in-flight requests coalesce (one job per load);
 * - the last valid snapshot stays in state across refresh failures, so a
 *   retry path shows stale data instead of emptying the section;
 * - `XAU → display fiat` uses the pair call so the backend's all-fiat
 *   USD pivot applies (TND, MAD, DZD…); a quotable-nowhere base (Empty)
 *   falls back to an explicitly labeled USD quote — a transport Error
 *   never falls back (stale/retry instead, never USD-masked).
 */
class GoldViewModel(
    private val repository: IRepository,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(GoldUiState())
    val state: StateFlow<GoldUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Display currency the current/last load targeted (upper-cased). */
    private var loadedCurrency: String = "USD"

    /** Monotonic generation: only the newest load may write state. */
    private var generation = 0

    /**
     * Load gold spot in [displayCurrency] (the stored market/display base).
     * Safe to call repeatedly: same currency with fresh data or an
     * in-flight load is a no-op unless [forceRefresh].
     */
    fun load(displayCurrency: String?, forceRefresh: Boolean = false) {
        val currency = displayCurrency
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
            ?: "USD"
        val current = _state.value
        if (!forceRefresh && currency == loadedCurrency) {
            val inFlight = current.loading
            val fresh = current.snapshot?.currency == currency && !current.stale
            if (inFlight || fresh) return
        }
        loadedCurrency = currency
        generation += 1
        val gen = generation
        loadJob?.cancel()
        _state.value = current.copy(
            loading = current.snapshot == null,
            stale = false,
            unavailable = false,
            requestedCurrency = currency,
        )
        loadJob = viewModelScope.launch {
            val snapshot = fetchSnapshot(currency)
            if (gen != generation) return@launch // outdated: discarded
            if (snapshot != null) {
                _state.update {
                    it.copy(
                        snapshot = snapshot,
                        loading = false,
                        stale = false,
                        unavailable = false,
                        usdFallback = snapshot.currency != currency,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        loading = false,
                        stale = it.snapshot != null,
                        unavailable = it.snapshot == null,
                        usdFallback = false,
                    )
                }
            }
        }
    }

    /** Section-local retry for the unavailable state. */
    fun retry() {
        load(loadedCurrency, forceRefresh = true)
    }

    /**
     * One spot quote in [currency], or the labeled USD fallback when that
     * pair is quotable nowhere (or returned an invalid value — zero,
     * negative, non-finite quotes are rejected, never displayed). Null on
     * transport failure — the caller keeps the previous snapshot and flags
     * stale/unavailable instead of masking with USD.
     */
    private suspend fun fetchSnapshot(currency: String): GoldSnapshot? =
        when (val response = repository.getLiveRateForPair(GOLD_CODE, currency)) {
            is MyResponse.Error -> null // transport failure — stale/retry, no USD masking
            is MyResponse.Success ->
                snapshotOf(currency, response.data) ?: usdFallbackFor(currency)
            is MyResponse.Empty -> usdFallbackFor(currency)
        }

    private fun snapshotOf(currency: String, spot: Double): GoldSnapshot? =
        spot.takeIf { it > 0.0 && it.isFinite() }
            ?.let { GoldSnapshot(currency = currency, spotPerTroyOunce = it, fetchedAtMs = nowMillis()) }

    /** Explicitly labeled USD quote — never for USD itself (nothing behind it). */
    private suspend fun usdFallbackFor(currency: String): GoldSnapshot? =
        if (currency == USD) null else fetchUsdFallback()

    private suspend fun fetchUsdFallback(): GoldSnapshot? =
        when (val response = repository.getLiveRateForPair(GOLD_CODE, USD)) {
            is MyResponse.Success -> snapshotOf(USD, response.data)
            else -> null
        }

    fun onCleared() {
        viewModelScope.cancel()
    }

    private companion object {
        const val GOLD_CODE = "XAU"
        const val USD = "USD"
    }
}
