package dali.hamza.shared.data.alerts

import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.LocalNotifier
import dali.hamza.shared.platform.currentEpochMillis
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Local rate-alert engine — the "job" that turns new data into
 * notifications.
 *
 * Each run fetches live rates once per distinct alert base and evaluates
 * every enabled alert against its previously observed rate:
 * - [RateAlertMode.PERIODIC] — fire when [RateAlert.intervalMinutes]
 *   elapsed since the last notification (1h / 2h digest)
 * - [RateAlertMode.THRESHOLD] — fire when the pair moved ±
 *   [RateAlert.thresholdPercent]% versus the previous check
 *
 * The freshly observed rate always becomes the stored baseline, so a
 * threshold alert re-arms from the new value (classic trailing trigger).
 * Runs are invoked from the platform schedulers (Android WorkManager /
 * iOS background refresh) and on app foreground; [runCheck] self-throttles
 * so frequent foreground resumes stay cheap.
 */
class RateAlertsEngine(
    private val repository: IRepository,
    currencyApi: CurrencyApi,
    private val sessionStorage: ISessionStorage,
    private val notifier: LocalNotifier,
    private val timeProvider: () -> Long = ::currentEpochMillis,
    /** Injectable fetcher (tests); default wraps the live API. */
    private val fetchRates: suspend (base: String) -> Map<String, Double>? = { base ->
        currencyApi.getLiveRates(base).getOrNull()?.rates
    },
) {

    /**
     * Foreground re-entry throttle timestamp (best-effort cache; reads
     * may be stale across threads, costing at most one extra pass).
     */
    private var lastCheckAt: Long = 0L

    /**
     * One evaluation pass. Returns the alerts that produced a
     * notification (empty when throttled, disabled or offline).
     */
    suspend fun runCheck(force: Boolean = false): List<RateAlert> =
        withContext(Dispatchers.Default) {
            val now = timeProvider()
            if (!force && lastCheckAt != 0L && now - lastCheckAt < MIN_CHECK_GAP_MS) {
                Napier.d(tag = TAG) { "check throttled (${(now - lastCheckAt) / 1000}s since last)" }
                return@withContext emptyList()
            }
            lastCheckAt = now

            if (!sessionStorage.getNotificationsEnabled()) {
                Napier.d(tag = TAG) { "check skipped: notifications disabled in preferences" }
                return@withContext emptyList()
            }
            if (!notifier.areNotificationsEnabled()) {
                Napier.d(tag = TAG) { "check skipped: OS notification permission not granted" }
                return@withContext emptyList()
            }

            val alerts = repository.getEnabledRateAlerts()
            if (alerts.isEmpty()) {
                Napier.d(tag = TAG) { "check skipped: no enabled alerts" }
                return@withContext emptyList()
            }

            val triggered = mutableListOf<RateAlert>()
            alerts.groupBy { it.base }.forEach { (base, group) ->
                val rates = fetchRates(base)
                if (rates == null) {
                    Napier.w(tag = TAG) { "fetch failed for base=$base — keeping previous state" }
                    return@forEach // offline / backend error → keep previous state
                }
                Napier.d(tag = TAG) { "base=$base: ${rates.size} rates fetched" }
                for (alert in group) {
                    val newRate = rates[alert.quote] ?: continue
                    val changePercent = alert.lastRate
                        ?.takeIf { it > 0.0 }
                        ?.let { previous -> (newRate - previous) / previous * 100.0 }

                    val shouldNotify = when (alert.mode) {
                        RateAlertMode.PERIODIC ->
                            now - alert.lastNotifiedAt >= alert.intervalMinutes * 60_000L
                        RateAlertMode.THRESHOLD ->
                            changePercent != null &&
                                abs(changePercent) >= alert.thresholdPercent &&
                                now - alert.lastNotifiedAt >= MIN_NOTIFY_GAP_MS
                    }
                    Napier.d(tag = TAG) {
                        "${alert.base}/${alert.quote}: new=$newRate prev=${alert.lastRate} " +
                            "change=${changePercent?.let { formatSignedPercent(it) }} notify=$shouldNotify"
                    }

                    if (shouldNotify) {
                        notifier.notify(
                            id = alert.id,
                            title = notificationTitle(alert, changePercent),
                            body = notificationBody(alert, newRate, changePercent),
                        )
                        repository.updateRateAlertState(alert.id, newRate, now)
                        triggered += alert.copy(lastRate = newRate, lastNotifiedAt = now)
                    } else {
                        // keep the baseline fresh even without a notification
                        repository.updateRateAlertState(alert.id, newRate, alert.lastNotifiedAt)
                    }
                }
            }
            triggered
        }

    private fun notificationTitle(alert: RateAlert, changePercent: Double?): String {
        val arrow = when {
            changePercent == null || changePercent == 0.0 -> "•"
            changePercent > 0 -> "▲"
            else -> "▼"
        }
        val change = changePercent?.let { " ${formatSignedPercent(it)}" } ?: ""
        return "${alert.base}/${alert.quote} $arrow$change"
    }

    private fun notificationBody(alert: RateAlert, newRate: Double, changePercent: Double?): String {
        val current = "1 ${alert.base} = ${formatRate(newRate)} ${alert.quote}"
        val previous = alert.lastRate?.let { ", was ${formatRate(it)}" } ?: ""
        return when (alert.mode) {
            RateAlertMode.PERIODIC ->
                "$current$previous — ${intervalLabel(alert.intervalMinutes)} check"
            RateAlertMode.THRESHOLD ->
                "Moved ${formatSignedPercent(changePercent ?: 0.0)} (your threshold " +
                    "${formatPercent(alert.thresholdPercent)}%) — $current$previous"
        }
    }

    companion object {
        private const val TAG = "RateAlertsEngine"

        /** Foreground re-entry throttle — background jobs always pass it. */
        const val MIN_CHECK_GAP_MS = 10L * 60_000L

        /** Minimum gap between two notifications for one threshold alert. */
        const val MIN_NOTIFY_GAP_MS = 15L * 60_000L

        fun intervalLabel(intervalMinutes: Long): String = "${intervalMinutes / 60}h"
    }
}

// ---- small pure formatters (no locale, common Kotlin) -------------------

internal fun formatRate(value: Double): String {
    val decimals = when {
        value >= 100.0 -> 2
        value >= 1.0 -> 4
        else -> 6
    }
    return formatFixed(value, decimals)
}

internal fun formatPercent(value: Double): String = formatFixed(value, 2)

internal fun formatSignedPercent(value: Double): String =
    (if (value >= 0.0) "+" else "") + formatFixed(abs(value), 2) + "%"

private fun formatFixed(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (value * factor).roundToLong()
    val whole = rounded / factor.toLong()
    val fraction = abs(rounded % factor.toLong())
    if (decimals == 0) return whole.toString()
    return "$whole.${fraction.toString().padStart(decimals, '0')}"
}
