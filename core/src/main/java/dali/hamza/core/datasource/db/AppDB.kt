package dali.hamza.core.datasource.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.db.entities.CurrencyEntity
import dali.hamza.core.datasource.db.entities.HistoricRatesCurrencyEntity
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.core.datasource.db.utilities.Converter

@Database(
    version = 1,
    entities = [
        CurrencyEntity::class,
        RatesCurrencyEntity::class,
        HistoricRatesCurrencyEntity::class
    ]
)
@TypeConverters(Converter::class)
abstract class AppDB : RoomDatabase() {
    abstract fun CurrencyDao(): CurrencyDao
    abstract fun RatesCurrencyDao(): RatesCurrencyDao
    abstract fun HistoricRateDao(): HistoricRateDao

}