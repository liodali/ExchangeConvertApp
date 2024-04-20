package mohamedali.hamza.database.dao

import androidx.room.Dao
import androidx.room.Query
import dali.hamza.domain.models.Currency
import kotlinx.coroutines.flow.Flow
import mohamedali.hamza.database.entities.CurrencyEntity

@Dao
interface CurrencyDao : AppDao<CurrencyEntity> {

    @Query("select * from currencyentity")
    fun flowListCurrencies(): Flow<List<Currency>>

    @Query("select * from currencyentity")
    fun getListCurrencies(): List<Currency>
}