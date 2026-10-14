package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dali.hamza.shared.data.widgets.WidgetsRepository
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

/**
 * Keeps the WorkManager refresh aligned with the configured widget set —
 * the same shape as the rate-alerts scheduler (`RateAlertSchedulerAndroid`).
 *
 * [update] starts/stops the unique 30-min periodic fetch; [refreshNow]
 * enqueues one immediate fetch (config change, app start with stale data).
 */
object WidgetRefreshScheduler {
    private const val PERIODIC_WORK = "market-widget-refresh-periodic"
    private const val NOW_WORK = "market-widget-refresh-now"

    /** Battery-friendly default; realtime policy uses the WorkManager floor. */
    private const val INTERVAL_MINUTES = 30L
    private const val INTERVAL_REALTIME_MINUTES = 15L

    fun update(context: Context, hasWidgets: Boolean, realtime: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        if (!hasWidgets) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            workManager.cancelUniqueWork(NOW_WORK)
            return
        }
        val interval = if (realtime) INTERVAL_REALTIME_MINUTES else INTERVAL_MINUTES
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            // UPDATE: a policy change (30↔15) must replace the old schedule
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MarketRateWidgetUpdateWorker>(interval, TimeUnit.MINUTES).build(),
        )
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MarketRateWidgetUpdateWorker>().build(),
        )
    }
}

/**
 * One pass: shared [WidgetsRepository.refreshSnapshots] (one live-rates call
 * per distinct base), then re-render every placed widget instance.
 */
class MarketRateWidgetUpdateWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val koin = KoinPlatform.getKoin() ?: return Result.failure()
        val repository = runCatching { koin.get<WidgetsRepository>() }.getOrNull()
            ?: return Result.failure()
        return runCatching {
            repository.refreshSnapshots()
            val manager = GlanceAppWidgetManager(applicationContext)
            // every widget look shares the refresh pass
            for (widget in listOf(
                MarketRateWidget(),
                MarketRateCardWidget(),
                MarketGridWidget(),
                QuickConvertWidget(),
            )) {
                runCatching {
                    manager.getGlanceIds(widget.javaClass).forEach { glanceId ->
                        widget.update(applicationContext, glanceId)
                    }
                }
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
