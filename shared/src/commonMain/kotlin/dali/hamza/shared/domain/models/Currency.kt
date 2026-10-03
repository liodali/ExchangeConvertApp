package dali.hamza.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val name: String,
    val fullCountryName: String
)

data class ExchangeRate(
    val name: String,
    val calculatedAmount: Double,
    val rate: Double,
    val time: Long, // Using Long timestamp for KMP compatibility
)

data class CurrencyRate(
    val name: String,
    val rate: Double,
    val time: Long, // Using Long timestamp for KMP compatibility
)
