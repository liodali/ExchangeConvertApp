package dali.hamza.core.datasource.db.dao

import androidx.room.Dao
import androidx.room.Query
import dali.hamza.core.datasource.db.entities.HistoricRatesCurrencyEntity
import dali.hamza.domain.models.Currency
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricRateDao:AppDao<HistoricRatesCurrencyEntity> {


    @Query("select * from RatesCurrencyEntity where selectedCurrency=:oldSelectCurrency")
    fun historicRate(oldSelectCurrency: String): Flow<List<HistoricRatesCurrencyEntity>>


}