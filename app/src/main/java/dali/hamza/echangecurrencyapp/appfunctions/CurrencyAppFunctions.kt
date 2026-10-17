package dali.hamza.echangecurrencyapp.appfunctions

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.domain.models.AlertSource
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.RateAlertScheduler
import dali.hamza.shared.ui.DeepLinkNav
import dali.hamza.shared.ui.Routes
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

/**
 * Dinarly's on-device MCP tool surface (Android intelligence
 * system). Exposes currency conversion, live rates, historical trends and
 * rate-alert management to system agents (Gemini and any app holding
 * `EXECUTE_APP_FUNCTIONS`), plus [openAppScreen] which hands back a
 * [PendingIntent] that opens the app's own UI deep-linked.
 *
 * Dependencies come from the Koin container started in
 * `ExchangeApplication` (Application.onCreate always precedes any bound
 * service in the process, so the graph is ready when a function runs).
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "CurrencyAppFunctionService",
    appFunctionXmlFileName = "currency_app_function_service",
)
abstract class BaseCurrencyAppFunctionService : AppFunctionService(), KoinComponent {

    private val repository: IRepository by inject()
    private val currencyApi: CurrencyApi by inject()
    private val alertScheduler: RateAlertScheduler by inject()

    /**
     * Convert an amount between two currencies using live exchange rates.
     * Also answers "what is the exchange rate of X to Y" — call it with
     * amount = 1 and read the `rate` field.
     *
     * @param amount The amount in the source currency. Must be positive.
     * @param from ISO code of the source currency (e.g. "USD"). Uppercased automatically.
     * @param to ISO code of the target currency (e.g. "EUR", "XAU" for gold). Uppercased automatically.
     * @return A [ConversionResult] with the converted amount and the unit rate.
     * @throws AppFunctionInvalidArgumentException If amount is not positive or from equals to.
     * @throws AppFunctionElementNotFoundException If either code is unknown — call "getCurrencies" to resolve valid codes.
     * @throws AppFunctionAppUnknownException If rates are temporarily unavailable (offline / backend unreachable). Suggest retrying later.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun convertCurrency(
        amount: Double,
        from: String,
        to: String,
    ): ConversionResult {
        if (amount <= 0.0 || !amount.isFinite()) {
            throw AppFunctionInvalidArgumentException("amount must be a positive number")
        }
        val source = requireCurrency(from, "from")
        val target = requireCurrency(to, "to")
        if (source == target) {
            throw AppFunctionInvalidArgumentException("from ($source) and to ($target) must differ")
        }
        val converted = try {
            currencyApi.convert(from = source, to = target, amount = amount)
                .getOrElse { error ->
                    throw AppFunctionAppUnknownException(
                        "Live rates unavailable for $source → $target: ${error.message ?: "network error"}",
                    )
                }
        } catch (e: CancellationException) {
            throw e
        }
        return converted?.let { value ->
            ConversionResult(
                from = source,
                to = target,
                amount = amount,
                convertedAmount = value,
                rate = value / amount,
            )
        } ?: throw AppFunctionAppUnknownException("No rate returned for $source → $target")
    }

    /**
     * List the currencies Dinarly supports for conversion and alerts.
     * Required workflow: call this whenever a user refers to a currency by
     * name ("euro", "dirham", "gold") to resolve it to a valid ISO code
     * before calling convertCurrency or addRateAlert.
     *
     * @param query Optional filter matched against codes and names (case-insensitive). Omit to list all.
     * @return Up to 40 [CurrencyInfo] entries sorted by code.
     * @throws AppFunctionInvalidArgumentException If a query is provided but matches nothing.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getCurrencies(query: String? = null): List<CurrencyInfo> {
        val currencies = when (val response = repository.getListCurrencies()) {
            is MyResponse.Success -> response.data
            else -> CurrenciesCatalog.currencies
        }
        val mapped = currencies.map { CurrencyInfo(code = it.name, name = it.fullCountryName) }
        val filtered = query
            ?.takeIf { it.isNotBlank() }
            ?.let { q ->
                mapped.filter {
                    it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
                }
            }
            ?: mapped
        if (filtered.isEmpty()) {
            throw AppFunctionInvalidArgumentException(
                "No currency matches \"$query\". Ask the user to rephrase or list all currencies with no query.",
            )
        }
        return filtered.sortedBy { it.code }.take(MAX_LIST_SIZE)
    }

    /**
     * Live exchange rates of one base currency against all (or selected) currencies.
     *
     * @param base Optional ISO code of the base currency; defaults to the user's base currency in the app.
     * @param symbols Optional ISO codes to include; omit for the full list (up to 40 entries).
     * @return [RateInfo] entries: 1 unit of base = `rate` units of each target currency.
     * @throws AppFunctionElementNotFoundException If base is unknown, or none of the requested symbols are available.
     * @throws AppFunctionAppUnknownException If rates are temporarily unavailable.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getExchangeRates(
        base: String? = null,
        symbols: List<String>? = null,
    ): List<RateInfo> {
        val currentBase = repository.getCurrentCurrency()
        val resolvedBase = base?.let { requireCurrency(it, "base") } ?: currentBase
        val wanted = symbols
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.uppercase().trim() }
            ?.filter { it != resolvedBase }
            ?.toSet()

        val raw: Map<String, Double> = if (resolvedBase == currentBase) {
            // cache-first path (respects the app's tier refresh window)
            when (val response = repository.getListRatesCurrencies(amount = 1.0)) {
                is MyResponse.Success -> response.data.associate { it.name to it.rate }
                is MyResponse.Error ->
                    throw AppFunctionAppUnknownException("Rates unavailable for $resolvedBase: ${response.error}")
                else -> throw AppFunctionAppUnknownException("Rates unavailable for $resolvedBase")
            }
        } else {
            currencyApi.getLiveRates(resolvedBase)
                .getOrElse { error ->
                    throw AppFunctionAppUnknownException(
                        "Live rates unavailable for $resolvedBase: ${error.message ?: "network error"}",
                    )
                }
                .rates.orEmpty()
        }
        if (raw.isEmpty()) {
            throw AppFunctionAppUnknownException("No rates stored for $resolvedBase — try again later")
        }
        val selected = wanted?.let { raw.filterKeys { it in wanted } } ?: raw
        if (selected.isEmpty()) {
            throw AppFunctionElementNotFoundException(
                "None of $symbols available for $resolvedBase. Call getCurrencies for valid codes.",
            )
        }
        return selected.entries
            .sortedBy { it.key }
            .take(MAX_LIST_SIZE)
            .map { (code, rate) ->
                RateInfo(
                    code = code,
                    name = CurrenciesCatalog.byCode(code)?.fullCountryName ?: code,
                    rate = rate,
                )
            }
    }

    /**
     * Historical daily exchange-rate series and statistics for one pair —
     * answers trend questions like "how did EUR/USD move this month?".
     *
     * @param base ISO code of the base currency.
     * @param quote ISO code of the quote (target) currency.
     * @param days Window length in days ending today; default 30, clamped to 1..365.
     * @return A [HistorySummary] with min/max/oldest/latest rates, the percentage change and a downsampled series.
     * @throws AppFunctionInvalidArgumentException If base equals quote.
     * @throws AppFunctionElementNotFoundException If either code is unknown.
     * @throws AppFunctionAppUnknownException If no history is available for the pair in the window.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getHistoricalRates(
        base: String,
        quote: String,
        days: Int? = 30,
    ): HistorySummary {
        val source = requireCurrency(base, "base")
        val target = requireCurrency(quote, "quote")
        if (source == target) {
            throw AppFunctionInvalidArgumentException("base ($source) and quote ($target) must differ")
        }
        val window = (days ?: 30).coerceIn(1, 365)
        val today = LocalDate.now()
        val from = today.minusDays((window - 1).toLong())

        val series = when (
            val response = repository.getHistoricalRates(
                base = source,
                symbol = target,
                from = from.toString(),
                to = today.toString(),
            )
        ) {
            is MyResponse.Success -> response.data
            is MyResponse.Error ->
                throw AppFunctionAppUnknownException(
                    "No historical rates for $source/$target in the last $window days",
                )
            else -> throw AppFunctionAppUnknownException("Historical rates unavailable for $source/$target")
        }
        val rates = series.map { it.rate }
        val oldest = rates.first()
        val latest = rates.last()
        return HistorySummary(
            base = source,
            quote = target,
            latestRate = latest,
            oldestRate = oldest,
            minRate = rates.min(),
            maxRate = rates.max(),
            changePercent = if (oldest != 0.0) (latest - oldest) / oldest * 100.0 else 0.0,
            points = downsample(series.map { HistoryPoint(date = it.date, rate = it.rate) }),
        )
    }

    /**
     * Create an on-device rate alert that posts a notification when the pair
     * is checked (PERIODIC: every 60 or 120 minutes; THRESHOLD: when the pair
     * moves ±thresholdPercent versus the previous check).
     * Required workflow: resolve currency names to ISO codes with
     * "getCurrencies" first. The alert appears in the app's Rate Alerts screen.
     *
     * @param base ISO code of the base currency, e.g. "EUR".
     * @param quote ISO code of the quote currency, e.g. "USD".
     * @param mode "PERIODIC" or "THRESHOLD".
     * @param intervalMinutes PERIODIC only: 60 or 120. Defaults to 60.
     * @param thresholdPercent THRESHOLD only: positive percent move that triggers the alert. Defaults to 0.5.
     * @return The created [RateAlertInfo] with source "LOCAL".
     * @throws AppFunctionInvalidArgumentException If codes are unknown or equal, mode/interval/threshold are invalid, the pair is already tracked, or the free-tier cap (2 alerts) is reached.
     * @throws AppFunctionElementNotFoundException If either code is unknown.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addRateAlert(
        base: String,
        quote: String,
        @AppFunctionStringValueConstraint(enumValues = ["PERIODIC", "THRESHOLD"])
        mode: String,
        intervalMinutes: Long? = null,
        thresholdPercent: Double? = null,
    ): RateAlertInfo {
        val source = requireCurrency(base, "base")
        val target = requireCurrency(quote, "quote")
        if (source == target) {
            throw AppFunctionInvalidArgumentException("base ($source) and quote ($target) must differ")
        }
        val alertMode = when (mode) {
            RateAlertMode.PERIODIC.name -> RateAlertMode.PERIODIC
            RateAlertMode.THRESHOLD.name -> RateAlertMode.THRESHOLD
            else -> throw AppFunctionInvalidArgumentException("mode must be PERIODIC or THRESHOLD")
        }
        val interval = intervalMinutes ?: RateAlert.INTERVAL_HOURLY
        val threshold = thresholdPercent ?: RateAlert.DEFAULT_THRESHOLD_PERCENT
        if (alertMode == RateAlertMode.PERIODIC && interval !in ALERT_INTERVALS) {
            throw AppFunctionInvalidArgumentException(
                "intervalMinutes must be one of $ALERT_INTERVALS (minutes)",
            )
        }
        if (alertMode == RateAlertMode.THRESHOLD && threshold <= 0.0) {
            throw AppFunctionInvalidArgumentException("thresholdPercent must be positive")
        }

        val created = repository
            .addRateAlert(
                RateAlert(
                    base = source,
                    quote = target,
                    mode = alertMode,
                    intervalMinutes = interval,
                    thresholdPercent = threshold,
                ),
            )
            .getOrElse { error ->
                throw AppFunctionInvalidArgumentException(
                    error.message ?: "Could not create the alert",
                )
            }
        // keep the WorkManager job aligned with the new alert so it starts
        // notifying without waiting for the next app open
        alertScheduler.update(repository.getEnabledRateAlerts().isNotEmpty())
        return created.toInfo()
    }

    /**
     * List the user's configured rate alerts (on-device and server-pushed).
     *
     * @return All alerts as [RateAlertInfo]; use alertId + source with "deleteRateAlert".
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listRateAlerts(): List<RateAlertInfo> {
        val local = repository.getRateAlerts().map { it.toInfo() }
        val server = runCatching { repository.getServerRateAlerts() }
            .getOrDefault(emptyList())
            .map { it.toInfo() }
        return local + server
    }

    /**
     * Delete one rate alert.
     *
     * @param alertId The id returned by "listRateAlerts" (or "addRateAlert").
     * @param source "LOCAL" (default) for on-device alerts or "SERVER" for pushed alerts.
     * @return A human-readable confirmation message.
     * @throws AppFunctionInvalidArgumentException If alertId is negative.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun deleteRateAlert(
        alertId: Long,
        @AppFunctionStringValueConstraint(enumValues = ["LOCAL", "SERVER"])
        source: String? = "LOCAL",
    ): String {
        if (alertId < 0) {
            throw AppFunctionInvalidArgumentException("alertId must not be negative")
        }
        if (source == AlertSource.SERVER.name) {
            repository.removeServerRateAlert(alertId)
        } else {
            repository.removeRateAlert(alertId)
            alertScheduler.update(repository.getEnabledRateAlerts().isNotEmpty())
        }
        return "Deleted rate alert #$alertId ($source)."
    }

    /**
     * Open a screen of the Dinarly app and hand back a launchable
     * [PendingIntent]. Launching it shows the app's own UI to the user —
     * use this whenever the user wants to see or continue working in the app
     * (e.g. after a conversion, or to review their alerts).
     *
     * @param screen Which screen to open: "home", "converter", "full-converter", "history", "rate-alerts", "widgets" or "account".
     * @param base Optional ISO code preselected as the source currency on converter screens, or the pair base on history.
     * @param quote Optional ISO code preselected as the target currency (converter) or pair quote (history).
     * @param amount Optional amount (as text, e.g. "120.50") prefilled on converter screens — the app converts automatically.
     * @return A [PendingIntent] that opens the requested screen; launch it to bring the app forward.
     * @throws AppFunctionInvalidArgumentException If screen is not one of the supported values.
     * @throws AppFunctionElementNotFoundException If base/quote codes are unknown.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun openAppScreen(
        @AppFunctionStringValueConstraint(enumValues = ["home", "converter", "full-converter", "history", "rate-alerts", "widgets", "account"])
        screen: String,
        base: String? = null,
        quote: String? = null,
        amount: String? = null,
    ): PendingIntent {
        val route = when (screen) {
            "home" -> Routes.HOME
            "converter" -> Routes.CONVERTER
            "full-converter" -> Routes.FULL_CONVERTER
            "history" -> Routes.HISTORY
            "rate-alerts" -> Routes.RATE_ALERTS
            "widgets" -> Routes.WIDGETS
            "account" -> Routes.ACCOUNT
            else -> throw AppFunctionInvalidArgumentException(
                "screen must be home, converter, full-converter, history, rate-alerts, widgets or account",
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(DeepLinkNav.EXTRA_ROUTE, route)
            base?.let { putExtra(DeepLinkNav.EXTRA_PAIR_BASE, requireCurrency(it, "base")) }
            quote?.let { putExtra(DeepLinkNav.EXTRA_PAIR_QUOTE, requireCurrency(it, "quote")) }
            amount?.let { putExtra(DeepLinkNav.EXTRA_AMOUNT, it) }
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // ---- helpers ---------------------------------------------------------

    /** Resolves a user/agent supplied code (case-insensitive) or throws. */
    private fun requireCurrency(code: String, param: String): String {
        val normalized = code.trim().uppercase()
        if (normalized.isEmpty()) {
            throw AppFunctionInvalidArgumentException("$param must be a currency code")
        }
        if (CurrenciesCatalog.byCode(normalized) == null) {
            throw AppFunctionElementNotFoundException(
                "Unknown currency code \"$code\" for $param. Call getCurrencies to list valid codes.",
            )
        }
        return normalized
    }

    /** Evenly samples [points] down to at most [MAX_LIST_SIZE] keeping the ends. */
    private fun <T> downsample(points: List<T>): List<T> {
        if (points.size <= MAX_LIST_SIZE) return points
        val step = (points.size - 1).toDouble() / (MAX_LIST_SIZE - 1)
        return (0 until MAX_LIST_SIZE).map { index ->
            points[Math.round(index * step).toInt().coerceAtMost(points.lastIndex)]
        }
    }

    private fun RateAlert.toInfo() = RateAlertInfo(
        alertId = id,
        base = base,
        quote = quote,
        mode = mode.name,
        intervalMinutes = intervalMinutes,
        thresholdPercent = thresholdPercent,
        enabled = enabled,
        source = source.name,
    )

    private companion object {
        /** Cap for list-shaped answers (rates / currencies / history points). */
        const val MAX_LIST_SIZE = 40

        /** The cadences the product offers for periodic alerts (minutes). */
        val ALERT_INTERVALS = listOf(RateAlert.INTERVAL_HOURLY, RateAlert.INTERVAL_TWO_HOURS)
    }
}
