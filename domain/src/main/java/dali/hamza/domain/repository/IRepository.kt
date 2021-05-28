package dali.hamza.domain.repository

import dali.hamza.domain.models.IResponse
import kotlinx.coroutines.flow.Flow

interface IRepository<T> {
    suspend fun getListCurrencies(): Flow<IResponse>
    suspend fun saveListCurrencies(): Flow<IResponse>
    suspend fun getExchangeRates(selectedCurrency: String): Flow<IResponse>
    suspend fun getListRatesCurrencies(): Flow<IResponse>
}