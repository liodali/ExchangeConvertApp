package mohamedali.hamza.database.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mohamedali.hamza.database.entities.CurrencyEntity

@Dao
interface CurrencyDao : AppDao<CurrencyEntity> {

    @Query("select * from currencyentity")
    fun flowListCurrencies(): Flow<List<CurrencyEntity>>

    @Query("select * from currencyentity")
    fun getListCurrencies(): List<CurrencyEntity>
}