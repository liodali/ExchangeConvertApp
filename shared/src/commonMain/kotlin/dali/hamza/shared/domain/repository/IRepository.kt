package dali.hamza.shared.domain.repository

import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
import kotlinx.coroutines.flow.Flow

interface IRepository {
    /**
     * Get list of currencies from local database
     */
    suspend fun getListCurrencies(): MyResponse<List<Currency>>

    /**
     * Save list of currencies from API to local database
     */
    suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>>

    /**
     * Save exchange rates of current currency
     */
    suspend fun saveExchangeRatesOfCurrentCurrency()

    /**
     * Get list of rates for the given amount
     */
    suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>>
    
    /**
     * Get current selected currency
     */
    suspend fun getCurrentCurrency(): String
}
