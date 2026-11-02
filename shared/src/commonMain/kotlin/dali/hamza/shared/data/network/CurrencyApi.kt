package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.LatestRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricRatesDataAPI
import dali.hamza.shared.data.network.models.HistoricSeriesDataAPI
import dali.hamza.shared.data.network.models.CurrencyNameDataAPI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

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
 * Auth: the anonymous-session Bearer is resolved ON DEMAND through
 * [bearerProvider] (PushSessionManager's cached/minted JWT), so data calls
 * never depend on the push-registration callback having run first. While
 * the server keeps `require-api-token` off, a missing session simply sends
 * the request bare (soft-launch tokenless tier).
 */
class CurrencyApi(
    private val httpClient: HttpClient,
    private val bearerProvider: suspend (forceRefresh: Boolean) -> String? = { null },
) {
    /**
     * Execute one data request under the anonymous session, healing a
     * stale cached JWT exactly once (plans/anonymous-session-lifecycle.md §3):
     * `401`, or the hard gate's `404` when a Bearer was sent, re-mints with
     * the SAME installId and retries the original request once. A second
     * rejection wins — no loop, and the anonymous identity never changes.
     *
     * @param retryOnNotFound false for endpoints outside the auth gate
     *   (a plain 404 there is an ordinary failure)
     */
    private suspend fun <T> callWithSession(
        retryOnNotFound: Boolean = true,
        block: suspend (bearer: String?) -> T,
    ): Result<T> {
        val bearer = bearerProvider(false)?.takeIf { it.isNotBlank() }
        val first = attempt(bearer, block)
        if (!first.isAuthRejection(bearer, retryOnNotFound)) return first
        val refreshed = bearerProvider(true)?.takeIf { it.isNotBlank() } ?: return first
        return attempt(refreshed, block)
    }

    private suspend fun <T> attempt(
        bearer: String?,
        block: suspend (bearer: String?) -> T,
    ): Result<T> = try {
        Result.success(block(bearer))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** `401` anywhere, or the hard auth gate's `404` while Bearer was sent. */
    private fun <T> Result<T>.isAuthRejection(bearer: String?, retryOnNotFound: Boolean): Boolean {
        if (bearer == null) return false
        val status = (exceptionOrNull() as? HttpStatusException)?.status ?: return false
        return status == 401 || (status == 404 && retryOnNotFound)
    }

    /**
     * Non-success response carrying its status. Checked BEFORE body
     * deserialization — an auth-gate 401/404 error body must not be parsed
     * as a successful data DTO and hide the failure.
     */
    private class HttpStatusException(val status: Int) : Exception("HTTP $status")

    private fun HttpResponse.ensureSuccess(): HttpResponse {
        if (!status.isSuccess()) throw HttpStatusException(status.value)
        return this
    }

    /** Attach the anonymous-session Bearer when one could be established. */
    private fun HttpRequestBuilder.attachSession(bearer: String?) {
        bearer?.takeIf { it.isNotBlank() }?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }

    /**
     * Current rates of [source] against all other currencies.
     * Response: `{base, time, rates: {"EUR": 0.88, ...}}`
     */
    suspend fun getLiveRates(source: String): Result<LatestRatesDataAPI> =
        callWithSession { bearer ->
            httpClient.get("latest") {
                attachSession(bearer)
                parameter("base", source)
            }.ensureSuccess().body<LatestRatesDataAPI>()
        }

    /**
     * Convert [amount] from [from] to [to]: the backend returns the pair's
     * rate with the amount already applied.
     */
    suspend fun convert(
        from: String,
        to: String,
        amount: Double,
    ): Result<Double?> = callWithSession { bearer ->
        httpClient.get("latest") {
            attachSession(bearer)
            parameter("base", from)
            parameter("symbol", to)
            parameter("amount", amount)
        }.ensureSuccess().body<LatestRatesDataAPI>().rates?.get(to)
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
    ): Result<HistoricSeriesDataAPI> = callWithSession { bearer ->
        httpClient.get("historic") {
            attachSession(bearer)
            parameter("base", base)
            parameter("symbol", symbol)
            parameter("from", from)
            parameter("to", to)
        }.ensureSuccess().body<HistoricSeriesDataAPI>()
    }

    /**
     * Rates of [source] for [date] (format: `yyyy-MM-dd`). Legacy
     * `historical` endpoint — outside the auth gate, so a 404 is never
     * treated as a stale-session signal.
     */
    suspend fun getHistoricRates(
        date: String,
        source: String,
    ): Result<HistoricRatesDataAPI> = callWithSession(retryOnNotFound = false) { bearer ->
        httpClient.get("historical") {
            attachSession(bearer)
            parameter("date", date)
            parameter("source", source)
        }.ensureSuccess().body<HistoricRatesDataAPI>()
    }

    /**
     * Supported currencies with display names, localized server-side for
     * [lang] (BCP-47) via the backend's CLDR names: `GET currencies?type=all&lang=…`.
     * Unknown codes (BTC, XAU…) come back with their stored English name.
     */
    suspend fun getCurrencies(lang: String): Result<List<CurrencyNameDataAPI>> =
        callWithSession { bearer ->
            httpClient.get("currencies") {
                attachSession(bearer)
                parameter("type", "all")
                parameter("lang", lang)
            }.ensureSuccess().body<List<CurrencyNameDataAPI>>()
        }
}
