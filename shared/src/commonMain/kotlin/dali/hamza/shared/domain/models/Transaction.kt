package dali.hamza.shared.domain.models

/**
 * A recorded currency conversion (Phase 4 — History cluster).
 *
 * `base` is the source currency ("from"), `quote` the target ("to"):
 * `amountBase` units of [base] became `amountQuote` units of [quote] at
 * [rate] (= amountQuote / amountBase) on [timestamp] (epoch millis).
 */
data class Transaction(
    val id: Long? = null,
    val base: String,
    val quote: String,
    val amountBase: Double,
    val amountQuote: Double,
    val rate: Double,
    val timestamp: Long,
    val direction: String = DIRECTION_EXCHANGE,
) {
    companion object {
        const val DIRECTION_EXCHANGE = "EXCHANGE"
    }
}

/**
 * One point of a daily historical series: ISO date `yyyy-MM-dd` + the
 * base→quote rate on that day.
 */
data class HistoricalRate(
    val date: String,
    val rate: Double,
)
