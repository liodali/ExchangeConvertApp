package dali.hamza.core.datasource.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class CurrencyEntity(
    @PrimaryKey val name: String,
    val fullCountryName: String
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = RatesCurrencyEntity::class,
            childColumns = ["name"],
            parentColumns = ["name"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RatesCurrencyEntity(
    @PrimaryKey val name: String,
    val rate: Double,
    val time: Date,
    val selectedCurrency: String
)

@Entity(
    primaryKeys = [
        "name",
        "selectedCurrency",
        "time"
    ]
)
data class HistoricRatesCurrencyEntity(
    val name: String,
    val rate: Double,
    val time: Date,
    val selectedCurrency: String

)
