package dali.hamza.core.datasource.network.models

import com.squareup.moshi.Json

data class CurrenciesDataAPI(
    @Json(name = "currencies") val currencies: Map<String,CurrencyData>,
)

data class CurrencyData(
    val currency :  Map<String, String>
)

data class RatesCurrenciesDataAPI(
    @Json(name = "quotes") val currencies: Map<String, Double>,
)

