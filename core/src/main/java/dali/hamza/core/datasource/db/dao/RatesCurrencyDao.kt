package dali.hamza.core.datasource.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import dali.hamza.core.datasource.db.entities.HistoricRatesCurrencyEntity
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface RatesCurrencyDao : AppDao<RatesCurrencyEntity> {


    @Query("select rate*:amount as calculatedAmount,rate,name,time from RatesCurrencyEntity where selectedCurrency=:currency")
    fun getListExchangeRatesCurrencies(
        amount: Double,
        currency: String
    ): Flow<List<ExchangeRate>>

    @Query("select rate*:amount as calculatedAmount,rate,name,time from RatesCurrencyEntity where selectedCurrency=:currency")
    fun getPagingListExchangeRatesCurrencies(
        amount: Double,
        currency: String
    ): PagingSource<Int,ExchangeRate>

    @Query("select * from RatesCurrencyEntity where selectedCurrency=:selectedCurrency")
    fun getListRatesByCurrencies(selectedCurrency: String): List<RatesCurrencyEntity>


    @Query("select * from RatesCurrencyEntity ")
    fun getLastListRatesCurrency(): List<RatesCurrencyEntity>


}