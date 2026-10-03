package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.LatestRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricSeriesDataAPI
import dali.hamza.shared.data.network.models.LiveRatesDataAPI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Ktor client for our exchange-api backend
 * (https://github.com/liodali/OpenExchangeRate — api.exchange.dev.adetify.com).
 *
 * Endpoints used:
 * - `latest?base=BASE[&symbol=SYM][&amount=N]` → rates of BASE against
 *   every supported currency (or the requested symbol); with `amount` the
 *   returned rate already includes it
 * - `historical?…` → Phase 4 (History cluster) — not migrated yet
 *
 * The backend needs no access key; the parameter is kept for compatibility.
 */
class CurrencyApi(
    private val httpClient: HttpClient,
    private val accessKey: String,
) {

    /**
     * Current rates of [source] against all other currencies.
     * Response: `{base, time, rates: {"EUR": 0.88, ...}}`
     */
    suspend fun getLiveRates(source: String): Result<LatestRatesDataAPI> {
        return try {
            val response = httpClient.get("latest") {
                parameter("access_key", accessKey)
                parameter("base", source)
            }
            Result.success(response.body<LatestRatesDataAPI>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert [amount] from [from] to [to]: the backend returns the pair's
     * rate with the amount already applied.
     */
    suspend fun convert(
        from: String,
        to: String,
        amount: Double,
    ): Result<Double?> {
        return try {
            val response = httpClient.get("latest") {
                parameter("access_key", accessKey)
                parameter("base", from)
                parameter("symbol", to)
                parameter("amount", amount)
            }
            Result.success(response.body<LatestRatesDataAPI>().rates?.get(to))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Daily historical series for one pair (Phase 4 — History cluster).
     * `GET historic?base=BASE&symbol=SYM&from=yyyy-MM-dd&to=yyyy-MM-dd`
     * → `{base, time, rates: {"2026-09-02": {"USD": 0.3435}, ...}}`
     */
    suspend fun getHistoricalSeries(
        base: String,
        symbol: String,
        from: String,
        to: String,
    ): Result<HistoricSeriesDataAPI> {
        return try {
            val response = httpClient.get("historic") {
                parameter("access_key", accessKey)
                parameter("base", base)
                parameter("symbol", symbol)
                parameter("from", from)
                parameter("to", to)
            }
            Result.success(response.body<HistoricSeriesDataAPI>())
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
