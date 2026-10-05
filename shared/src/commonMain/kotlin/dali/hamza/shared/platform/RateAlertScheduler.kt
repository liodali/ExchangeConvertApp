package dali.hamza.shared.platform

/**
 * Keeps the platform background job aligned with the alert state:
 * - Android: WorkManager periodic (+ one immediate) check
 * - iOS: BGAppRefreshTask request, best-effort (iOS schedules it when it
 *   sees fit; foreground checks cover the rest)
 *
 * Called by the Rate Alerts UI after every mutation and at app start.
 */
interface RateAlertScheduler {
    /**
     * [hasActiveAlerts] = at least one enabled alert exists — start/refresh
     * the background job; `false` cancels it.
     */
    fun update(hasActiveAlerts: Boolean)
}

expect fun createRateAlertScheduler(): RateAlertScheduler
