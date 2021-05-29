package dali.hamza.domain.repository

import dali.hamza.domain.models.IResponse
import kotlinx.coroutines.flow.Flow

interface IRepository {
    /**
     * recuperation of list currencies from local database
     */
    suspend fun getListCurrencies(): Flow<IResponse>

    /**
     * recuperation of list currencies from
     * https://api.currencylayer.com/list
     * and save it in local database
     */
    suspend fun saveListCurrencies(): Flow<IResponse>

    /**
     * recuperation the list of rates from
     * https://api.currencylayer.com/live?source={selectedCurrency}
     * and save it in local Database
     */
    suspend fun saveExchangeRatesOfCurrentCurrency()

    /**
     * calculate the exchange rates of selected currency
     */
    suspend fun getListRatesCurrencies(amount: Double): Flow<IResponse>
}