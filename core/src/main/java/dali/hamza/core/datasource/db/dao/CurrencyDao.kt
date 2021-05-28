package dali.hamza.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import dali.hamza.core.datasource.db.entities.CurrencyEntity
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.ExchangeRate
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao : AppDao<CurrencyEntity> {

    @Query("select * from currencyentity")
    fun getListCurrencies(): Flow<List<Currency>>



}