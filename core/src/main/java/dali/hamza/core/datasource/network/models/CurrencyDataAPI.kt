package dali.hamza.core.datasource.network.models

import com.squareup.moshi.Json

data class CurrenciesDataAPI(
    @Json(name = "success") val success : Boolean,
    @Json(name = "symbols") val symbols: Map<String,Map<String,CurrencyData>>,
)

data class CurrencyData(
    @Json(name = "description")   val description:String,
    @Json(name = "code")  val code:String,
    //val currency :  Map<String, String>
)

data class RatesCurrenciesDataAPI(
    @Json(name = "success") val success : Boolean,
    @Json(name = "quotes") val quotes: Map<String, RateData>,
)

data class RateData(
    val rate :  Map<String, Double>
)

