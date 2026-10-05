package dali.hamza.shared.domain.repository

import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.Transaction
import kotlinx.coroutines.flow.Flow

interface IRepository {
    /**
     * Get list of currencies (local catalog / database)
     */
    suspend fun getListCurrencies(): MyResponse<List<Currency>>

    /**
     * Save list of currencies to local database
     */
    suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>>

    /**
     * Fetch latest rates of the current currency from the API and store them locally
     * (rate-limited: refreshes at most every [REFRESH_INTERVAL_MS])
     */
    suspend fun saveExchangeRatesOfCurrentCurrency()

    /**
     * Get list of exchange rates for the stored base currency,
     * with [amount] applied to each rate
     */
    suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>>

    /**
     * Get current selected currency (ISO code)
     */
    suspend fun getCurrentCurrency(): String

    /**
     * Set current selected currency (ISO code)
     */
    suspend fun setCurrentCurrency(value: String)

    /**
     * Force a refresh of the exchange rates, bypassing the rate-limit cache
     */
    suspend fun refreshExchangeRates()

    /**
     * Profile display name (top bar) — additive, for the Account phase.
     */
    suspend fun getUsername(): String = ""

    /**
     * Daily historical series for a pair (Phase 4 — History cluster).
     * [from]/[to] are ISO dates `yyyy-MM-dd`; points are sorted by date.
     */
    suspend fun getHistoricalRates(
        base: String,
        symbol: String,
        from: String,
        to: String,
    ): MyResponse<List<HistoricalRate>>

    /**
     * Daily historical series for several symbols at once (rates grid deltas).
     * The backend accepts a comma-joined `symbol` list; results are keyed by
     * symbol, each series sorted by date.
     */
    suspend fun getHistoricalRates(
        base: String,
        symbols: List<String>,
        from: String,
        to: String,
    ): MyResponse<Map<String, List<HistoricalRate>>>

    /**
     * All recorded conversion transactions, newest first.
     */
    suspend fun getTransactions(): List<Transaction>

    /**
     * Record a conversion transaction (single call from the
     * `SharedViewModel.convert()` success path).
     */
    suspend fun recordTransaction(transaction: Transaction)

    /**
     * Remove all recorded transactions (guest-mode "Clear Local Ledger").
     */
    suspend fun clearTransactions()

    /**
     * All configured rate alerts (local notifications), newest first.
     */
    suspend fun getRateAlerts(): List<RateAlert>

    /**
     * Enabled rate alerts only — the alert engine's working set.
     */
    suspend fun getEnabledRateAlerts(): List<RateAlert>

    /**
     * Create a rate alert. Fails when the free-tier cap
     * ([RateAlert.maxAlertsForTier]) is reached or the pair is already
     * tracked.
     */
    suspend fun addRateAlert(alert: RateAlert): Result<RateAlert>

    suspend fun removeRateAlert(id: Long)

    suspend fun setRateAlertEnabled(id: Long, enabled: Boolean)

    /**
     * Persist the engine's evaluation state (observed rate + throttle
     * timestamp) for one alert.
     */
    suspend fun updateRateAlertState(id: Long, lastRate: Double?, lastNotifiedAt: Long)
}
