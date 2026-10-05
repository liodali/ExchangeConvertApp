package dali.hamza.shared.domain.models

/**
 * Local rate alert (Account → Preferences → Rate Alerts).
 *
 * A free-tier (guest) account can track at most [maxAlertsForTier] pairs;
 * each alert is evaluated by [dali.hamza.shared.data.alerts.RateAlertsEngine]
 * against the previously observed rate for the pair:
 * - [RateAlertMode.PERIODIC] — notify every [intervalMinutes] (1h / 2h)
 * - [RateAlertMode.THRESHOLD] — notify as soon as the pair moves
 *   ±[thresholdPercent]% versus the previous check
 *
 * Notifications are fully local — no push server involved.
 */
enum class RateAlertMode {
    PERIODIC,
    THRESHOLD,
}

data class RateAlert(
    val id: Long = 0L,
    val base: String,
    val quote: String,
    val mode: RateAlertMode = RateAlertMode.PERIODIC,
    val intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES,
    val thresholdPercent: Double = DEFAULT_THRESHOLD_PERCENT,
    val enabled: Boolean = true,
    /** Last observed rate for the pair — the baseline for change checks. */
    val lastRate: Double? = null,
    /** Epoch millis of the last posted notification (throttle). */
    val lastNotifiedAt: Long = 0L,
    val createdAt: Long = 0L,
) {
    companion object {
        const val INTERVAL_HOURLY = 60L
        const val INTERVAL_TWO_HOURS = 120L
        const val DEFAULT_INTERVAL_MINUTES = INTERVAL_HOURLY
        const val DEFAULT_THRESHOLD_PERCENT = 0.5

        /** Repository failure copy — free-tier cap. */
        const val LIMIT_MESSAGE = "Alert limit reached — the free plan tracks up to 2 pairs."

        /** Repository failure copy — one alert per pair. */
        const val DUPLICATE_MESSAGE = "You already track this pair."

        /**
         * Alert cap per tier: guests (free) get 2, Sovereign login will
         * unlock more. Enforced in `IRepository.addRateAlert`.
         */
        fun maxAlertsForTier(tier: DataTier): Int = when (tier) {
            DataTier.GUEST -> 2
            DataTier.SOVEREIGN -> 10
        }
    }
}
