package dali.hamza.shared.platform

import dali.hamza.shared.data.storage.ISessionStorage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

private const val APP_GROUP = "group.dali.hamza.ExchangeConvertApp"
private const val ACTIVE_KEY = "widgets.ios.active.json"
private const val MARKETS_KEY = "widgets.ios.markets.json"

/**
 * Swift hook for the WidgetKit call — the shared framework does not link
 * WidgetKit; the app side sets this in AppDelegate and performs
 * `WidgetCenter.shared.reloadAllTimelines()` (main thread).
 */
object IosWidgetRelay {
    var onRefresh: (() -> Unit)? = null
}

/**
 * iOS bridge: exports the app's market pairs into the App Group (the grid
 * widget mirrors them) and relays refresh requests to WidgetKit via Swift.
 * Reads the extension's render reports for the "on your home screen" list —
 * iOS offers no widget enumeration, this is detection-by-render.
 */
object IosWidgetsHostBridge : WidgetsHostBridge {
    override val canPin: Boolean = false
    override val canManage: Boolean = false

    override fun requestPin(kind: WidgetKind, base: String, quote: String) = Unit

    override fun reconfigure(id: String) = Unit

    override fun refreshAll() {
        exportMarketPairs()
        IosWidgetRelay.onRefresh?.invoke()
    }

    override fun applyRefreshPolicy(realtime: Boolean) = refreshAll()

    override fun activeWidgetPairs(): List<Pair<String, String>> {
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return emptyList()
        val raw = defaults.stringForKey(ACTIVE_KEY) ?: return emptyList()
        val pairs = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                ListSerializer(String.serializer()),
                raw,
            )
        }.getOrNull() ?: return emptyList()
        return pairs.mapNotNull { pair ->
            pair.split("/").takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.distinct()
    }

    /** Market-preference pairs for the iOS grid widget (App Group). */
    private fun exportMarketPairs() {
        val koin = org.koin.mp.KoinPlatform.getKoin() ?: return
        val storage = runCatching { koin.get<ISessionStorage>() }.getOrNull() ?: return
        val base = storage.getCurrency().ifBlank { "USD" }
        val pairs = storage.getMarketPreferences().filter { it != base }.take(3)
            .map { "$base/$it" }
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return
        runCatching {
            defaults.setObject(
                Json.encodeToString(ListSerializer(String.serializer()), pairs),
                forKey = MARKETS_KEY,
            )
        }
    }
}

actual fun createWidgetsHostBridge(): WidgetsHostBridge = IosWidgetsHostBridge
