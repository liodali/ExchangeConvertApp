package dali.hamza.shared.platform

/**
 * iOS actual — intentionally a no-op: BGTaskScheduler task requests must
 * be submitted from Swift (NSError**-style API, awkward from Kotlin).
 * The Swift host owns scheduling — see
 * `iosApp/ExchangeConvertApp/RateAlertsBackground.swift`:
 * - registration at launch (AppDelegate)
 * - `RateAlertsBackground.schedule()` on scenePhase .background and re-armed
 *   inside the task handler, gated on active alerts via
 *   `RateAlertsBridgeKt.hasRateAlerts`
 * - the engine itself runs through `RateAlertsBridgeKt.runCheckNow`
 */
internal class RateAlertSchedulerIos : RateAlertScheduler {
    override fun update(hasActiveAlerts: Boolean) = Unit
}

actual fun createRateAlertScheduler(): RateAlertScheduler = RateAlertSchedulerIos()
