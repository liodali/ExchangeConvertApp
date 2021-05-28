package dali.hamza.domain

data class Currency(
    val name: String,
    val fullCountryName: String
)


data class CurrencyRate(
    val name: String,
    val rate: Double
)
