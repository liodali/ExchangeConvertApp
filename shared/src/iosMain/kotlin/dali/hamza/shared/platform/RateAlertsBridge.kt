package dali.hamza.shared.platform

import dali.hamza.shared.data.alerts.RateAlertsEngine
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Completion-based entry points for the Swift hosts (AppDelegate /
 * scenePhase): run one [RateAlertsEngine] pass and report how many alerts
 * fired. Used by the BGAppRefreshTask handler and on every foreground
 * activation. [hasRateAlerts] gates the Swift-side background scheduling.
 */
fun runRateAlertsCheckNow(onDone: (Int) -> Unit) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    scope.launch {
        val triggered = resolveEngine()?.let { engine ->
            runCatching { engine.runCheck() }.getOrDefault(emptyList())
        }
        onDone(triggered?.size ?: 0)
    }
}

fun hasRateAlerts(onResult: (Boolean) -> Unit) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    scope.launch {
        val active = runCatching {
            KoinPlatform.getKoin()?.get<IRepository>()
                ?.getEnabledRateAlerts()
                ?.isNotEmpty() ?: false
        }.getOrDefault(false)
        onResult(active)
    }
}

private fun resolveEngine(): RateAlertsEngine? {
    val koin = KoinPlatform.getKoin() ?: return null
    return runCatching { koin.get<RateAlertsEngine>() }.getOrNull()
}
