package dali.hamza.core.datasource.network.models

import com.squareup.moshi.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys


data class CurrencyData(
    @Json(name = "description") val description: String,
    @Json(name = "code") val code: String,
    //val currency :  Map<String, String>
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RatesCurrenciesDataAPI(
    @Json(name = "base") val base: String,
    @Json(name = "rates") val rates: Map<String, Double>,
)

data class RateData(
    val rate: Map<String, Double>
)

