package dali.hamza.shared.data.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the exchangerate.host API (currencylayer-compatible endpoints):
 * GET /live?source=BASE, GET /convert, GET /historical?date=...
 * All requests require an `access_key` query parameter.
 */
@Serializable
data class ApiError(
    @SerialName("code") val code: Int? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("info") val info: String? = null,
) {
    fun message(): String = info ?: type ?: "Unknown API error"
}

@Serializable
data class LiveRatesDataAPI(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("quotes") val quotes: Map<String, Double>? = null,
    @SerialName("error") val error: ApiError? = null,
)

@Serializable
data class ConvertQuery(
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("amount") val amount: Double,
)

@Serializable
data class ConvertInfo(
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("quote") val quote: Double? = null,
)

@Serializable
data class ConvertDataAPI(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("query") val query: ConvertQuery? = null,
    @SerialName("info") val info: ConvertInfo? = null,
    @SerialName("result") val result: Double? = null,
    @SerialName("error") val error: ApiError? = null,
)

@Serializable
data class HistoricRatesDataAPI(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("historical") val historical: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("quotes") val quotes: Map<String, Double>? = null,
    @SerialName("error") val error: ApiError? = null,
)
