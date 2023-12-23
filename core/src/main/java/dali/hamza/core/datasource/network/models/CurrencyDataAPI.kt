package dali.hamza.core.datasource.network.models

import com.squareup.moshi.Json

data class CurrenciesDataAPI(
    @Json(name = "success") val success : Boolean,
    @Json(name = "currencies") val currencies: Map<String,Map<String,String>>,
)

data class CurrencyData(
    @Json(name = "description")   val description:String,
    @Json(name = "code")  val code:String,
    //val currency :  Map<String, String>
)

data class RatesCurrenciesDataAPI(
    @Json(name = "success") val success : Boolean,
    @Json(name = "source") val source : String,
    @Json(name = "quotes") val quotes: Map<String, Map<String, Double>>,
)

data class RateData(
    val rate :  Map<String, Double>
)

