package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.ConvertDataAPI
import dali.hamza.shared.data.network.models.HistoricRatesDataAPI
import dali.hamza.shared.data.network.models.LiveRatesDataAPI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Ktor client for [exchangerate.host](https://api.exchangerate.host).
 *
 * The host now requires an access key on every request (currencylayer-compatible API).
 * Working endpoints with the current plan:
 * - `live?source=BASE`        → current rates of BASE against every other currency
 * - `convert?from=&to=&amount=` → single conversion
 * - `historical?date=&source=`  → rates of a specific date
 *
 * Currency names come from [dali.hamza.shared.data.CurrenciesCatalog] (static, local).
 */
class CurrencyApi(
    private val httpClient: HttpClient,
    private val accessKey: String,
) {

    /**
     * Current rates of [source] against all other currencies.
     * Quote keys are concatenated, e.g. `source=USD` produces `USDAED` → rate.
     */
    suspend fun getLiveRates(source: String): Result<LiveRatesDataAPI> {
        return try {
            val response = httpClient.get("live") {
                parameter("access_key", accessKey)
                parameter("source", source)
            }
            Result.success(response.body<LiveRatesDataAPI>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert [amount] from [from] to [to] using the API's own quote.
     */
    suspend fun convert(
        from: String,
        to: String,
        amount: Double,
    ): Result<ConvertDataAPI> {
        return try {
            val response = httpClient.get("convert") {
                parameter("access_key", accessKey)
                parameter("from", from)
                parameter("to", to)
                parameter("amount", amount)
            }
            Result.success(response.body<ConvertDataAPI>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rates of [source] for [date] (format: `yyyy-MM-dd`).
     */
    suspend fun getHistoricRates(
        date: String,
        source: String,
    ): Result<HistoricRatesDataAPI> {
        return try {
            val response = httpClient.get("historical") {
                parameter("access_key", accessKey)
                parameter("date", date)
                parameter("source", source)
            }
            Result.success(response.body<HistoricRatesDataAPI>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
