package dali.hamza.shared.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.data.widgets.WidgetsRepository
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.WidgetConfig
import dali.hamza.shared.domain.models.WidgetSnapshot
import dali.hamza.shared.platform.DefaultWidgetsHostBridge
import dali.hamza.shared.platform.WidgetKind
import dali.hamza.shared.platform.WidgetsHostBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Account → Widgets state (`plans/widgets.md`). Listing/forgetting hits the
 * shared store; adding delegates to the platform [WidgetsHostBridge] (pin
 * request on Android, guide on iOS). Opening the screen refreshes snapshots
 * so the management list shows fresh rates and placed widgets update too.
 */
class WidgetsViewModel(
    private val repository: WidgetsRepository,
    private val storage: ISessionStorage? = null,
    private val bridge: WidgetsHostBridge = DefaultWidgetsHostBridge,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Currency catalog for the add-dialog pickers. */
    val currencies: List<Currency> = CurrenciesCatalog.currencies

    /**
     * Quick-select pairs for the add dialog: configured widget pairs first,
     * then the Account market preferences (base + tracked codes). Empty
     * before onboarding/market selection.
     */
    var suggestedPairs: List<Pair<String, String>> by mutableStateOf(emptyList())
        private set

    /** Configured widget instances, newest last. */
    var configs by mutableStateOf<List<WidgetConfig>>(emptyList())
        private set

    /** Latest snapshot per "BASE/QUOTE" pair (shared across instances). */
    var snapshots by mutableStateOf<Map<String, WidgetSnapshot>>(emptyMap())
        private set

    val hostBridge: WidgetsHostBridge get() = bridge

    /** One-shot UI messages (forget confirmation). */
    val events = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Pairs the platform detected on the home screen (iOS render reports). */
    var activePairs by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    init {
        activePairs = bridge.activeWidgetPairs()
        refresh()
    }

    /** Refresh policy (Account → Widgets): realtime-ish vs battery-friendly. */
    var realtime by mutableStateOf(storage?.getWidgetRealtime() ?: false)
        private set

    fun toggleRealtime() {
        val next = !realtime
        storage?.setWidgetRealtime(next)
        realtime = next
        viewModelScope.launch { runCatching { bridge.applyRefreshPolicy(next) } }
    }

    /** Re-read store + fetch fresh rates (never throws; offline keeps cache). */
    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refreshSnapshots() }
            reload()
            runCatching { bridge.refreshAll() }
        }
    }

    private fun reload() {
        // hidden configs are forgotten-but-still-placed — never listed
        configs = repository.configs().filterNot { it.removed }
        snapshots = repository.readSnapshotsForPairs()
        val fromPrefs = storage?.let { store ->
            val base = store.getCurrency().ifBlank { "USD" }
            store.getMarketPreferences().filter { it != base }.map { base to it }
        }.orEmpty()
        val fromWidgets = configs.map { it.base to it.quote }
        suggestedPairs = (fromWidgets + fromPrefs).distinct()
    }

    fun snapshotFor(config: WidgetConfig): WidgetSnapshot? =
        snapshots["${config.base}/${config.quote}"]

    /** Pin request — the pair is stashed for the widget's self-configuration. */
    fun add(kind: WidgetKind, base: String, quote: String, gridPairs: List<Pair<String, String>> = emptyList()) {
        if (kind != WidgetKind.MARKET_GRID && base == quote) return
        when {
            kind == WidgetKind.MARKET_GRID && gridPairs.isNotEmpty() ->
                repository.savePendingPairs(gridPairs.take(3))
            kind != WidgetKind.MARKET_GRID ->
                repository.savePendingPair(base, quote)
            // default grid: no config — it mirrors market preferences
            else -> Unit
        }
        bridge.requestPin(kind, base, quote)
    }

    fun reconfigure(config: WidgetConfig) = bridge.reconfigure(config.id)

    /**
     * Forget the configuration — the placed instance (if any) renders its
     * dead "Removed" state until the user long-presses it away on the
     * launcher (providers cannot unplace widgets).
     */
    fun forget(config: WidgetConfig) {
        repository.forgetConfig(config.id)
        reload()
        viewModelScope.launch {
            events.emit(dali.hamza.shared.ui.theme.LedgerStrings.Widgets.FORGOTTEN)
            runCatching { bridge.refreshAll() }
        }
    }
}
