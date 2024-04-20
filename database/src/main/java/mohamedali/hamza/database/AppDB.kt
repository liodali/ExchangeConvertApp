package mohamedali.hamza.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mohamedali.hamza.database.dao.CurrencyDao
import mohamedali.hamza.database.dao.HistoricRateDao
import mohamedali.hamza.database.dao.RatesCurrencyDao
import mohamedali.hamza.database.entities.CurrencyEntity
import mohamedali.hamza.database.entities.HistoricRatesCurrencyEntity
import mohamedali.hamza.database.entities.RatesCurrencyEntity
import mohamedali.hamza.database.utilities.Converter

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