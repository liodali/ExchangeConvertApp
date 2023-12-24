package dali.hamza.domain.models

import java.util.*

data class Currency(
    val name: String,
    val fullCountryName: String
)
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
