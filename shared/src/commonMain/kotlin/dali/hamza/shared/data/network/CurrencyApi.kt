package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.LatestRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricSeriesDataAPI
import dali.hamza.shared.data.network.models.LiveRatesDataAPI
import dali.hamza.shared.data.network.models.CurrencyNameDataAPI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.HttpRequestBuilder
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
 * Auth: optional anonymous-session Bearer (see attachSession) — the old
 * vestigial access_key query param was removed with the 0.12.1 app line.
 */
class CurrencyApi(
    private val httpClient: HttpClient,
    private val bearerProvider: () -> String? = { null },
) {
    /**
     * Anonymous session Bearer (PushSessionManager's cached JWT). Attached
     * when present so the backend applies the per-install tier; requests
     * before the launch loop mints the session simply go bare — fine while
     * the server keeps REQUIRE_API_TOKEN off (soft launch until app 0.14.0).
     */
    private fun HttpRequestBuilder.attachSession() {
        bearerProvider()?.takeIf { it.isNotBlank() }?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }


    /**
     * Current rates of [source] against all other currencies.
     * Response: `{base, time, rates: {"EUR": 0.88, ...}}`
     */
    suspend fun getLiveRates(source: String): Result<LatestRatesDataAPI> {
        return try {
            val response = httpClient.get("latest") {
                attachSession()
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
                attachSession()
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
                attachSession()
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
                attachSession()
                parameter("date", date)
                parameter("source", source)
            }
            Result.success(response.body<HistoricRatesDataAPI>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supported currencies with display names, localized server-side for
     * [lang] (BCP-47) via the backend's CLDR names: `GET currencies?type=all&lang=…`.
     * Unknown codes (BTC, XAU…) come back with their stored English name.
     */
    suspend fun getCurrencies(lang: String): Result<List<CurrencyNameDataAPI>> {
        return try {
            val response = httpClient.get("currencies") {
                attachSession()
                parameter("type", "all")
                parameter("lang", lang)
            }
            Result.success(response.body<List<CurrencyNameDataAPI>>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
