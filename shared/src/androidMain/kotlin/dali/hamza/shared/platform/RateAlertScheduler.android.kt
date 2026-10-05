package dali.hamza.shared.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.data.alerts.RateAlertsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

/**
 * Android actual — WorkManager hosts the rate-alert job:
 * - a unique periodic check every [CHECK_INTERVAL_MINUTES] (the engine
 *   enforces each alert's own 1h/2h interval on top of it)
 * - one immediate one-time check whenever [update] sees active alerts,
 *   so opening the app with alerts configured re-evaluates right away
 */
internal class RateAlertSchedulerAndroid : RateAlertScheduler {

    override fun update(hasActiveAlerts: Boolean) {
        val context = AndroidAppContext.appContext ?: return
        val workManager = WorkManager.getInstance(context)
        if (!hasActiveAlerts) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            workManager.cancelUniqueWork(ONE_TIME_WORK)
            return
        }
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RateAlertCheckWorker>().build(),
        )
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RateAlertCheckWorker>(CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build(),
        )
    }

    companion object {
        private const val PERIODIC_WORK = "rate-alerts-periodic"
        private const val ONE_TIME_WORK = "rate-alerts-now"
        private const val CHECK_INTERVAL_MINUTES = 30L
    }
}

actual fun createRateAlertScheduler(): RateAlertScheduler = RateAlertSchedulerAndroid()

/**
 * WorkManager entry point — resolves the shared [RateAlertsEngine] from
 * the app's Koin container.
 */
class RateAlertCheckWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val koin = KoinPlatform.getKoin() ?: return Result.failure()
        val engine = runCatching { koin.get<RateAlertsEngine>() }.getOrNull()
            ?: return Result.failure()
        return runCatching { engine.runCheck(force = true) }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}

/**
 * Foreground hook for the host activity (MainActivity.onResume): runs one
 * engine pass. The engine's own throttle keeps repeated resumes cheap.
 */
object RateAlertsAndroid {

    fun checkOnForeground() {
        val koin = KoinPlatform.getKoin() ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            runCatching { koin.get<RateAlertsEngine>().runCheck() }
        }
    }
}
