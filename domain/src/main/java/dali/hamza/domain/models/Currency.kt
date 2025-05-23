package dali.hamza.domain.models

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Currency(
    val name: String,
    val fullCountryName: String
) {
    constructor(json: Map<String, String>) : this(
        name = json["currency"]!!,
        fullCountryName = json["name"]!!
    )
}

data class ExchangeRate(
    val name: String,
    val calculatedAmount: Double,
    val rate: Double,
    val time: Date,
)

data class CurrencyRate(
    val name: String,
    val rate: Double,
    val time: Date,
)
