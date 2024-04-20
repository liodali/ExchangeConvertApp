package mohamedali.hamza.database.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mohamedali.hamza.database.entities.HistoricRatesCurrencyEntity

@Dao
interface HistoricRateDao : AppDao<HistoricRatesCurrencyEntity> {


    @Query("select * from HistoricRatesCurrencyEntity where selectedCurrency=:oldSelectCurrency")
    fun historicRate(oldSelectCurrency: String): Flow<List<HistoricRatesCurrencyEntity>>


}