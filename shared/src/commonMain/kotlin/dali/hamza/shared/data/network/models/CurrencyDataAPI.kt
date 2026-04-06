package dali.hamza.shared.data.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatesCurrenciesDataAPI(
    @SerialName("base") val base: String,
    @SerialName("rates") val rates: Map<String, Double>,
)

@Serializable
data class CurrencyApiModel(
    @SerialName("currency") val code: String,
    @SerialName("name") val description: String
)
