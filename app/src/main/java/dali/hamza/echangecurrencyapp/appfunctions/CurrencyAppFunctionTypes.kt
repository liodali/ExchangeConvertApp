package dali.hamza.echangecurrencyapp.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * A tradable currency known to Dinarly.
 *
 * [code] is the ISO-4217-style code agents must pass to the other functions
 * (fiat like "EUR", metals like "XAU" gold / "XAG" silver, crypto like "BTC").
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CurrencyInfo(
    /** The ISO-4217-style currency code, e.g. "EUR", "USD", "XAU" (gold), "BTC". */
    val code: String,
    /** Human-readable name, e.g. "Euro", "Gold (troy ounce)". */
    val name: String,
)

/**
 * The result of converting [amount] of currency [from] into [to].
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ConversionResult(
    /** Source currency code the amount was converted from. */
    val from: String,
    /** Target currency code the amount was converted into. */
    val to: String,
    /** The original amount in the source currency. */
    val amount: Double,
    /** The converted amount in the target currency. */
    val convertedAmount: Double,
    /** Exchange rate for 1 unit of [from] expressed in [to]. */
    val rate: Double,
)

/**
 * One live exchange rate of a base currency against a target currency.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RateInfo(
    /** Target currency code, e.g. "EUR". */
    val code: String,
    /** Human-readable name of the target currency. */
    val name: String,
    /** How much 1 unit of the base currency is worth in this currency. */
    val rate: Double,
)

/**
 * One daily point of a historical exchange-rate series.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class HistoryPoint(
    /** The day of the observation, ISO format "yyyy-MM-dd". */
    val date: String,
    /** The exchange rate on that day. */
    val rate: Double,
)

/**
 * Summary of a currency pair's historical movement (daily series).
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class HistorySummary(
    /** Base currency code of the pair. */
    val base: String,
    /** Quote (target) currency code of the pair. */
    val quote: String,
    /** Most recent observed rate in the window. */
    val latestRate: Double,
    /** Oldest observed rate in the window. */
    val oldestRate: Double,
    /** Lowest rate observed in the window. */
    val minRate: Double,
    /** Highest rate observed in the window. */
    val maxRate: Double,
    /** Percentage change from the oldest to the latest rate (positive = target strengthened). */
    val changePercent: Double,
    /** Downsampled daily series (at most 20 points, always including first and last). */
    val points: List<HistoryPoint>,
)

/**
 * A configured rate alert that notifies the user when checked.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RateAlertInfo(
    /** Unique id of the alert — pass this to "deleteRateAlert". */
    val alertId: Long,
    /** Base currency code of the watched pair. */
    val base: String,
    /** Quote currency code of the watched pair. */
    val quote: String,
    /** Alert mode: "PERIODIC" (recurring check) or "THRESHOLD" (fires on a % move). */
    val mode: String,
    /** For PERIODIC: checking interval in minutes (60 or 120). */
    val intervalMinutes: Long,
    /** For THRESHOLD: percentage move that triggers the alert. */
    val thresholdPercent: Double,
    /** Whether the alert is currently active. */
    val enabled: Boolean,
    /** Where the alert is evaluated: "LOCAL" (on-device engine) or "SERVER" (push). */
    val source: String,
)
