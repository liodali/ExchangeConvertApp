package dali.hamza.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import dali.hamza.core.datasource.db.entities.HistoricRatesCurrencyEntity
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface RatesCurrencyDao : AppDao<RatesCurrencyEntity> {


    @Query("select rate*:amount as calculedAmount,name,time from RatesCurrencyEntity where selectedCurrency=:currency")
    fun getListExchangeRatesCurrencies(
        amount: Double,
        currency: String
    ): Flow<List<ExchangeRate>>

    @Query("select * from RatesCurrencyEntity where selectedCurrency=:selectedCurreny")
    fun getListRatesByCurrencies(selectedCurreny: String): List<RatesCurrencyEntity>


    @Query("select time from RatesCurrencyEntity where selectedCurrency=:selectedCurreny ORDER BY time DESC LIMIT 1")
    fun getLastTimeUpdated(selectedCurreny: String): Date?


}