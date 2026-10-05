package dali.hamza.shared.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.LocalNotifier
import dali.hamza.shared.platform.RateAlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rate Alerts cluster state — KMP-safe, no platform ViewModel base
 * (same shape as [AccountViewModel]). Every mutation re-aligns the
 * background [RateAlertScheduler] with the active-alert state.
 */
class RateAlertsViewModel(
    private val repository: IRepository,
    private val scheduler: RateAlertScheduler,
    private val notifier: LocalNotifier,
    private val storage: ISessionStorage,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Configured alerts, newest first. */
    var alerts by mutableStateOf<List<RateAlert>>(emptyList())
        private set

    /** Tier alert cap — 2 on the free (guest) plan. */
    var maxAlerts by mutableStateOf(RateAlert.maxAlertsForTier(storage.getDataTier()))
        private set

    /** OS-level notification permission state. */
    var permissionGranted by mutableStateOf(true)
        private set

    /** Transient action feedback (limit reached, duplicate pair…). */
    var message by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            alerts = repository.getRateAlerts()
            maxAlerts = RateAlert.maxAlertsForTier(storage.getDataTier())
            permissionGranted = notifier.areNotificationsEnabled()
            scheduler.update(alerts.any { it.enabled })
        }
    }

    /** Ask the OS for notification permission (Android 13+ / iOS). */
    fun requestPermission() {
        notifier.requestPermission { granted ->
            permissionGranted = granted
        }
    }

    fun addAlert(
        base: String,
        quote: String,
        mode: RateAlertMode,
        intervalMinutes: Long,
        thresholdPercent: Double,
    ) {
        viewModelScope.launch {
            // best effort: surface the system prompt while creating the
            // alert — the engine no-ops until permission is actually granted
            if (!notifier.areNotificationsEnabled()) {
                notifier.requestPermission { granted -> permissionGranted = granted }
            }
            repository.addRateAlert(
                RateAlert(
                    base = base,
                    quote = quote,
                    mode = mode,
                    intervalMinutes = intervalMinutes,
                    thresholdPercent = thresholdPercent,
                )
            ).fold(
                onSuccess = {
                    message = null
                    alerts = repository.getRateAlerts()
                    scheduler.update(alerts.any { it.enabled })
                },
                onFailure = { failure -> message = failure.message },
            )
        }
    }

    fun removeAlert(id: Long) {
        viewModelScope.launch {
            repository.removeRateAlert(id)
            message = null
            alerts = repository.getRateAlerts()
            scheduler.update(alerts.any { it.enabled })
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRateAlertEnabled(id, enabled)
            alerts = repository.getRateAlerts()
            scheduler.update(alerts.any { it.enabled })
        }
    }

    fun clearMessage() {
        message = null
    }
}
