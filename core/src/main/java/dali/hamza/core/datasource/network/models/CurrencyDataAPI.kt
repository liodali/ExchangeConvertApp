package dali.hamza.core.datasource.network.models

import com.squareup.moshi.Json
import kotlinx.serialization.Serializable


data class CurrencyData(
    @Json(name = "description") val description: String,
    @Json(name = "code") val code: String,
    //val currency :  Map<String, String>
)

@Serializable
data class RatesCurrenciesDataAPI(
    @Json(name = "base") val source: String,
    @Json(name = "rates") val quotes: Map<String, Double>,
)

data class RateData(
    val rate: Map<String, Double>
)

