package dali.hamza.shared.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.AlertServiceUnavailableException
import dali.hamza.shared.domain.models.AlertSource
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.LocalNotifier
import dali.hamza.shared.platform.RateAlertScheduler
import dali.hamza.shared.ui.theme.LedgerStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rate Alerts cluster state — KMP-safe, no platform ViewModel base
 * (same shape as [AccountViewModel]). Every mutation re-aligns the
 * background [RateAlertScheduler] with the *local* alert state (server
 * alerts are evaluated in the cloud — plans/server-push-alerts.md §5.1).
 *
 * An alert lives in exactly one place: **on device** (local engine, all
 * modes, "as it was") or **server push** (guest: one alert, 2h cadence).
 * When the push service is unreachable, a server add falls back to a
 * local alert — the flow never dead-ends.
 */
class RateAlertsViewModel(
    private val repository: IRepository,
    private val scheduler: RateAlertScheduler,
    private val notifier: LocalNotifier,
    private val storage: ISessionStorage,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Server-evaluated alerts (push-delivered), newest first. */
    var serverAlerts by mutableStateOf<List<RateAlert>>(emptyList())
        private set

    /** On-device alerts (local engine), newest first. */
    var localAlerts by mutableStateOf<List<RateAlert>>(emptyList())
        private set

    /** Everything the screen renders — push section first. */
    val alerts: List<RateAlert>
        get() = serverAlerts + localAlerts

    /** Current account tier — drives the add-dialog cadence choices. */
    val tier: DataTier
        get() = storage.getDataTier()

    /** Cap of the on-device list ([RateAlert.maxAlertsForTier]). */
    var maxLocalAlerts by mutableStateOf(RateAlert.maxAlertsForTier(storage.getDataTier()))
        private set

    /** Cap of the server-push list ([RateAlert.maxServerAlertsForTier]). */
    var maxServerAlerts by mutableStateOf(RateAlert.maxServerAlertsForTier(storage.getDataTier()))
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
            localAlerts = repository.getRateAlerts()
            // best effort: offline renders an empty push list, no dead-ends
            serverAlerts = repository.getServerRateAlerts()
            maxLocalAlerts = RateAlert.maxAlertsForTier(tier)
            maxServerAlerts = RateAlert.maxServerAlertsForTier(tier)
            permissionGranted = notifier.areNotificationsEnabled()
            scheduler.update(localAlerts.any { it.enabled })
        }
    }

    /** Ask the OS for notification permission (Android 13+ / iOS). */
    fun requestPermission() {
        notifier.requestPermission { granted ->
            permissionGranted = granted
        }
    }

    /**
     * Create an alert in the chosen engine. A push add that can't reach
     * the alert service is saved on device instead (same parameters) so
     * the user never loses the action.
     */
    fun addAlert(
        base: String,
        quote: String,
        mode: RateAlertMode,
        intervalMinutes: Long,
        thresholdPercent: Double,
        source: AlertSource,
    ) {
        viewModelScope.launch {
            // best effort: surface the system prompt while creating the
            // alert — neither engine can notify without the permission
            if (!notifier.areNotificationsEnabled()) {
                notifier.requestPermission { granted -> permissionGranted = granted }
            }
            val result = when (source) {
                AlertSource.SERVER -> repository.addServerRateAlert(
                    RateAlert(
                        base = base,
                        quote = quote,
                        mode = mode,
                        intervalMinutes = intervalMinutes,
                        thresholdPercent = thresholdPercent,
                    )
                )
                AlertSource.LOCAL -> addLocal(base, quote, mode, intervalMinutes, thresholdPercent)
            }
            var note: String? = null
            val failure = if (
                source == AlertSource.SERVER &&
                result.exceptionOrNull() is AlertServiceUnavailableException
            ) {
                // push unreachable → same alert on device, say so
                val fallback = addLocal(base, quote, mode, intervalMinutes, thresholdPercent)
                if (fallback.isSuccess) {
                    note = LedgerStrings.RateAlerts.PUSH_FALLBACK_NOTE
                    null
                } else {
                    fallback.exceptionOrNull()
                }
            } else {
                result.exceptionOrNull()
            }
            message = failure?.message ?: note
            localAlerts = repository.getRateAlerts()
            serverAlerts = repository.getServerRateAlerts()
            scheduler.update(localAlerts.any { it.enabled })
        }
    }

    private suspend fun addLocal(
        base: String,
        quote: String,
        mode: RateAlertMode,
        intervalMinutes: Long,
        thresholdPercent: Double,
    ): Result<RateAlert> =
        repository.addRateAlert(
            RateAlert(
                base = base,
                quote = quote,
                mode = mode,
                intervalMinutes = intervalMinutes,
                thresholdPercent = thresholdPercent,
            )
        )

    fun removeAlert(alert: RateAlert) {
        viewModelScope.launch {
            when (alert.source) {
                AlertSource.SERVER -> repository.removeServerRateAlert(alert.id)
                AlertSource.LOCAL -> repository.removeRateAlert(alert.id)
            }
            message = null
            localAlerts = repository.getRateAlerts()
            serverAlerts = repository.getServerRateAlerts()
            scheduler.update(localAlerts.any { it.enabled })
        }
    }

    fun setEnabled(alert: RateAlert, enabled: Boolean) {
        viewModelScope.launch {
            when (alert.source) {
                AlertSource.SERVER -> repository.setServerRateAlertEnabled(alert.id, enabled)
                AlertSource.LOCAL -> repository.setRateAlertEnabled(alert.id, enabled)
            }
            localAlerts = repository.getRateAlerts()
            serverAlerts = repository.getServerRateAlerts()
            scheduler.update(localAlerts.any { it.enabled })
        }
    }

    fun clearMessage() {
        message = null
    }
}
