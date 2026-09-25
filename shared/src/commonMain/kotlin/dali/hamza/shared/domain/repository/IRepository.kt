package dali.hamza.shared.domain.repository

import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
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
}
