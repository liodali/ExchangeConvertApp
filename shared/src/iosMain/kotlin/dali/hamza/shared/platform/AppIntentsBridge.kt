package dali.hamza.shared.platform

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.models.AlertSource
import dali.hamza.shared.domain.repository.IRepository
import kotlin.math.roundToLong
import org.koin.mp.KoinPlatform

// ----------------------------------------------------------------------------
// App Intents bridge (iOS): envelope-style DTOs + suspend entry points the
// Swift intent layer (iosApp/ExchangeConvertApp/AppIntents/*.swift) calls
// with async/await. Mirrors the Android AppFunctions surface
// (app/.../appfunctions/CurrencyAppFunctions.kt) — keep the two in sync.
//
// Design notes:
// - Errors come back as `errorMessage` on the envelope instead of thrown
//   Kotlin exceptions (KotlinException → NSError bridging is lossy; the
//   Swift layer just renders the string in a dialog).
// - Koin is warm before any intent runs: AppDelegate didFinishLaunching
//   calls doInitSharedKoin (App Intents execute in the app process, after
//   launch). A missing container still degrades to an error envelope.
// - iOS alert scheduling is Swift-owned (BGTaskScheduler — the Kotlin
//   RateAlertScheduler actual is a no-op), so alert mutations made here
//   re-arm on the next scenePhase .background; the foreground engine pass
//   covers the rest.
// ----------------------------------------------------------------------------

/** One conversion result (or [errorMessage] set). */
class ConversionRow(
    val from: String,
    val to: String,
    val amount: Double,
    val convertedAmount: Double,
    val rate: Double,
    val errorMessage: String? = null,
)

/** One supported currency (ISO code + display name). */
class CurrencyRow(
    val code: String,
    val name: String,
)

class CurrenciesResult(
    val currencies: List<CurrencyRow>,
    val errorMessage: String? = null,
)

/** One live rate: 1 unit of the base = [rate] units of [code]. */
class RateRow(
    val code: String,
    val name: String,
    val rate: Double,
)

class RatesResult(
    val base: String,
    val rates: List<RateRow>,
    val errorMessage: String? = null,
)

/** One daily point of a history series (ISO date yyyy-MM-dd). */
class HistoryPointRow(
    val date: String,
    val rate: Double,
)

class HistoryResult(
    val base: String,
    val quote: String,
    val latestRate: Double,
    val oldestRate: Double,
    val minRate: Double,
    val maxRate: Double,
    val changePercent: Double,
    val points: List<HistoryPointRow>,
    val errorMessage: String? = null,
)

/** One configured rate alert. */
class AlertRow(
    val alertId: Long,
    val base: String,
    val quote: String,
    val mode: String,
    val intervalMinutes: Long,
    val thresholdPercent: Double,
    val enabled: Boolean,
    val source: String,
)

class AlertsResult(
    val alerts: List<AlertRow>,
    val errorMessage: String? = null,
)

class AlertMutationResult(
    val alert: AlertRow?,
    val errorMessage: String? = null,
)

class MutationResult(
    val success: Boolean,
    val errorMessage: String? = null,
)

/** Cap for list-shaped answers (parity with the Android service). */
private const val MAX_LIST_SIZE = 40

/** The cadences the product offers for periodic alerts (minutes). */
private val ALERT_INTERVALS = listOf(RateAlert.INTERVAL_HOURLY, RateAlert.INTERVAL_TWO_HOURS)

object AppIntentsBridge {

    /**
     * Convert [amount] between two currencies (live). [from]/[to] accept any
     * casing; validated against the catalog. Errors → [ConversionRow.errorMessage].
     */
    suspend fun convert(amount: Double, from: String, to: String): ConversionRow {
        val (source, target, err) = validPair(from, to)
        if (err != null) {
            return ConversionRow(source, target, amount, 0.0, 0.0, err)
        }
        if (amount <= 0.0 || !amount.isFinite()) {
            return ConversionRow(source, target, amount, 0.0, 0.0, "Amount must be a positive number")
        }
        val api = api() ?: return unavailable(source, target, amount)
        val converted = api.convert(from = source, to = target, amount = amount).getOrNull()
        val value = converted ?: return ConversionRow(
            from = source, to = target, amount = amount, convertedAmount = 0.0, rate = 0.0,
            errorMessage = "Live rates unavailable for $source → $target. Check your connection and try again.",
        )
        return ConversionRow(source, target, amount, value, value / amount)
    }

    /**
     * Supported currencies (optionally filtered by [query] over code/name);
     * caps at 40 rows. Empty filter result is an error envelope.
     */
    suspend fun currencies(query: String?): CurrenciesResult {
        val repo = repository() ?: return CurrenciesResult(emptyList(), notReady())
        val all = when (val response = repo.getListCurrencies()) {
            is MyResponse.Success -> response.data
            else -> CurrenciesCatalog.currencies
        }.map { CurrencyRow(code = it.name, name = it.fullCountryName) }
        val q = query?.trim().orEmpty()
        val filtered = if (q.isEmpty()) all else all.filter {
            it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
        }
        if (filtered.isEmpty()) {
            return CurrenciesResult(emptyList(), "No currency matches \"$q\".")
        }
        return CurrenciesResult(filtered.sortedBy { it.code }.take(MAX_LIST_SIZE))
    }

    /**
     * Live rates of [base] (defaults to the user's base) against all or
     * [symbols] currencies. Cache-first when [base] matches the user's base
     * (respects the tier refresh window), live API otherwise.
     */
    suspend fun rates(base: String?, symbols: List<String>?): RatesResult {
        val repo = repository() ?: return RatesResult(base ?: "", emptyList(), notReady())
        val currentBase = runCatching { repo.getCurrentCurrency() }.getOrDefault("USD")
        val resolvedBase = base?.trim()?.uppercase().orEmpty().ifEmpty { currentBase }
        if (CurrenciesCatalog.byCode(resolvedBase) == null) {
            return RatesResult(resolvedBase, emptyList(), unknownCode(resolvedBase))
        }
        val wanted = symbols
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotEmpty() && it != resolvedBase }
            ?.toSet()
        val raw: Map<String, Double> = if (resolvedBase == currentBase) {
            when (val response = repo.getListRatesCurrencies(amount = 1.0)) {
                is MyResponse.Success -> response.data.associate { it.name to it.rate }
                is MyResponse.Error -> return RatesResult(resolvedBase, emptyList(), "Rates unavailable for $resolvedBase.")
                else -> return RatesResult(resolvedBase, emptyList(), "Rates unavailable for $resolvedBase.")
            }
        } else {
            api()?.getLiveRates(resolvedBase)?.getOrNull()?.rates.orEmpty()
        }
        if (raw.isEmpty()) {
            return RatesResult(resolvedBase, emptyList(), "No rates stored for $resolvedBase — try again later.")
        }
        val selected = (wanted?.let { raw.filterKeys { code -> code in it } } ?: raw)
            .filterKeys { CurrenciesCatalog.byCode(it) != null }
        if (selected.isEmpty()) {
            return RatesResult(resolvedBase, emptyList(), "None of the requested currencies are available for $resolvedBase.")
        }
        return RatesResult(
            base = resolvedBase,
            rates = selected.entries.sortedBy { it.key }.take(MAX_LIST_SIZE).map { (code, rate) ->
                RateRow(code = code, name = CurrenciesCatalog.byCode(code)?.fullCountryName ?: code, rate = rate)
            },
        )
    }

    /**
     * Daily history stats + downsampled series for a pair. [from]/[to] are
     * ISO dates `yyyy-MM-dd` (computed in Swift — Kotlin/Native has no
     * java.time; Calendar there is trivial).
     */
    suspend fun history(base: String, quote: String, from: String, to: String): HistoryResult {
        val (source, target, err) = validPair(base, quote)
        if (err != null) {
            return HistoryResult(source, target, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), err)
        }
        val repo = repository()
            ?: return HistoryResult(source, target, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), notReady())
        val series = when (val response = repo.getHistoricalRates(source, target, from, to)) {
            is MyResponse.Success -> response.data
            is MyResponse.Error -> return HistoryResult(
                source, target, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(),
                "No historical rates for $source/$target in that window.",
            )
            else -> return HistoryResult(
                source, target, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), "Historical rates unavailable.",
            )
        }
        if (series.isEmpty()) {
            return HistoryResult(
                source, target, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(),
                "No historical rates for $source/$target in that window.",
            )
        }
        val rates = series.map { it.rate }
        val oldest = rates.first()
        val latest = rates.last()
        val points = series.map { HistoryPointRow(date = it.date, rate = it.rate) }
        val sampled = if (points.size <= MAX_LIST_SIZE) points else {
            val step = (points.size - 1).toDouble() / (MAX_LIST_SIZE - 1)
            (0 until MAX_LIST_SIZE).map { i -> points[(i * step).roundToLong().toInt().coerceAtMost(points.lastIndex)] }
        }
        return HistoryResult(
            base = source,
            quote = target,
            latestRate = latest,
            oldestRate = oldest,
            minRate = rates.min(),
            maxRate = rates.max(),
            changePercent = if (oldest != 0.0) (latest - oldest) / oldest * 100.0 else 0.0,
            points = sampled,
        )
    }

    /**
     * Create an on-device (LOCAL) rate alert — same validation and caps as
     * the in-app add-alert flow: valid pair, one alert per pair, free-tier
     * cap of 2, PERIODIC intervals 60/120 minutes only.
     */
    suspend fun addAlert(
        base: String,
        quote: String,
        mode: String,
        intervalMinutes: Long,
        thresholdPercent: Double,
    ): AlertMutationResult {
        val (source, target, err) = validPair(base, quote)
        if (err != null) {
            return AlertMutationResult(null, err)
        }
        val alertMode = when (mode.uppercase()) {
            RateAlertMode.PERIODIC.name -> RateAlertMode.PERIODIC
            RateAlertMode.THRESHOLD.name -> RateAlertMode.THRESHOLD
            else -> return AlertMutationResult(null, "Mode must be PERIODIC or THRESHOLD.")
        }
        if (alertMode == RateAlertMode.PERIODIC && intervalMinutes !in ALERT_INTERVALS) {
            return AlertMutationResult(null, "Interval must be 60 or 120 minutes.")
        }
        if (alertMode == RateAlertMode.THRESHOLD && thresholdPercent <= 0.0) {
            return AlertMutationResult(null, "Threshold must be positive.")
        }
        val repo = repository() ?: return AlertMutationResult(null, notReady())
        val created = repo.addRateAlert(
            RateAlert(
                base = source,
                quote = target,
                mode = alertMode,
                intervalMinutes = intervalMinutes,
                thresholdPercent = thresholdPercent,
            ),
        ).getOrElse { failure -> return AlertMutationResult(null, failure.message ?: "Could not create the alert.") }
        return AlertMutationResult(created.toRow())
    }

    /** All configured alerts: LOCAL rows + SERVER (push) rows, best effort. */
    suspend fun listAlerts(): AlertsResult {
        val repo = repository() ?: return AlertsResult(emptyList(), notReady())
        val local = runCatching { repo.getRateAlerts().map { it.toRow() } }.getOrDefault(emptyList())
        val server = runCatching { repo.getServerRateAlerts().map { it.toRow() } }.getOrDefault(emptyList())
        return AlertsResult(local + server)
    }

    /** Delete one alert by id — [source] "LOCAL" (default) or "SERVER". */
    suspend fun deleteAlert(alertId: Long, source: String): MutationResult {
        val repo = repository() ?: return MutationResult(false, notReady())
        return if (source.uppercase() == AlertSource.SERVER.name) {
            runCatching { repo.removeServerRateAlert(alertId) }
                .map { MutationResult(true) }
                .getOrElse { MutationResult(false, it.message ?: "Could not delete the alert.") }
        } else {
            runCatching { repo.removeRateAlert(alertId) }
                .map { MutationResult(true) }
                .getOrElse { MutationResult(false, it.message ?: "Could not delete the alert.") }
        }
    }

    // ---- helpers ---------------------------------------------------------

    private fun repository(): IRepository? =
        KoinPlatform.getKoin()?.get<IRepository>()

    private fun api(): CurrencyApi? = KoinPlatform.getKoin()?.get<CurrencyApi>()

    private fun notReady() = "The app's shared runtime is not initialised yet — open the app once and retry."

    private fun unknownCode(code: String) =
        "Unknown currency code \"$code\". Pick a currency from the list."

    /** Validates + normalizes a pair; third element is an error message or null when valid. */
    private fun validPair(base: String, quote: String): Triple<String, String, String?> {
        val source = base.trim().uppercase()
        val target = quote.trim().uppercase()
        if (source.isEmpty() || target.isEmpty()) {
            return Triple(source, target, "Pick both currencies of the pair.")
        }
        if (source == target) {
            return Triple(source, target, "The two currencies must differ.")
        }
        if (CurrenciesCatalog.byCode(source) == null) return Triple(source, target, unknownCode(source))
        if (CurrenciesCatalog.byCode(target) == null) return Triple(source, target, unknownCode(target))
        return Triple(source, target, null)
    }

    private fun unavailable(from: String, to: String, amount: Double) = ConversionRow(
        from = from, to = to, amount = amount, convertedAmount = 0.0, rate = 0.0, errorMessage = notReady(),
    )

    private fun RateAlert.toRow() = AlertRow(
        alertId = id,
        base = base,
        quote = quote,
        mode = mode.name,
        intervalMinutes = intervalMinutes,
        thresholdPercent = thresholdPercent,
        enabled = enabled,
        source = source.name,
    )
}
