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

/**
 * Where an alert is evaluated (plans/server-push-alerts.md §2: an alert
 * lives in exactly ONE place — never both, so no double notifications).
 *
 * - [LOCAL] — the on-device engine (existing alerts; SQLDelight row ids)
 * - [SERVER] — the exchange-api evaluator, delivered via FCM push
 *   (Phase 2: the guest add-alert flow creates these; backend row ids)
 */
enum class AlertSource {
    LOCAL,
    SERVER,
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
    /** Where this alert is evaluated — [AlertSource.LOCAL] by default. */
    val source: AlertSource = AlertSource.LOCAL,
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
         * Repository failure copy — server-push alert cap (guest = 1,
         * plans/server-push-alerts.md tier matrix).
         */
        const val SERVER_LIMIT_MESSAGE =
            "Alert limit reached — the guest plan tracks 1 pushed alert."

        /** Repository failure copy — alerts backend unreachable. */
        const val SERVER_UNAVAILABLE_MESSAGE =
            "Couldn't reach the alert service — check your connection and try again."

        /**
         * Alert cap per tier: guests (free) get 2, Sovereign login will
         * unlock more. Enforced in `IRepository.addRateAlert`.
         */
        fun maxAlertsForTier(tier: DataTier): Int = when (tier) {
            DataTier.GUEST -> 2
            DataTier.SOVEREIGN -> 10
        }

        /**
         * Server-evaluated alert cap per tier (parity with the backend's
         * `AlertRules.alertCap`): guest 1 / logged 3 / base 10 (future).
         * Enforced server-side; mirrored for the add-dialog cap UI.
         */
        fun maxServerAlertsForTier(tier: DataTier): Int = when (tier) {
            DataTier.GUEST -> 1
            DataTier.SOVEREIGN -> 3
        }

        /**
         * Server-push cadence choices per tier (product decision, Oct 2026):
         * - guest: **2h only** (the digest budget — hourly would just be
         *   throttled into 2h digests anyway)
         * - Sovereign (login): 1h or 2h
         * - paid base tier: free cadence — defined later with the auth work
         *
         * Local (on-device) alerts keep all modes for every tier.
         */
        fun serverIntervalsForTier(tier: DataTier): List<Long> = when (tier) {
            DataTier.GUEST -> listOf(INTERVAL_TWO_HOURS)
            DataTier.SOVEREIGN -> listOf(INTERVAL_HOURLY, INTERVAL_TWO_HOURS)
        }
    }
}

/** Transport-level failure reaching the alerts backend (offline, DNS). */
class AlertServiceUnavailableException :
    IllegalStateException(RateAlert.SERVER_UNAVAILABLE_MESSAGE)
