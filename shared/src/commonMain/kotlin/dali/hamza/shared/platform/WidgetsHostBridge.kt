package dali.hamza.shared.platform

/** Widget kinds offered in the add flow (mirrors the picker entries). */
enum class WidgetKind { MARKET_CARD, MARKET_WAVE, MARKET_GRID, QUICK_CONVERT }

/**
 * Platform capabilities for widget management (Account → Widgets screen,
 * `plans/widgets.md`). The screen degrades gracefully when the host can't
 * do something:
 *
 * - **Android**: pin via `requestPinAppWidget` (launcher support varies),
 *   configured instances are enumerable (our store), reconfigure relaunches
 *   the config activity.
 * - **iOS** (until the WidgetKit phase): apps can neither pin nor enumerate
 *   widget instances — the screen shows an add-from-home-screen guide and
 *   a timeline refresh button.
 */
interface WidgetsHostBridge {
    /** The host can request widget pinning (Android launchers, mostly yes). */
    val canPin: Boolean

    /** Configured widget instances are enumerable/managable from the app. */
    val canManage: Boolean

    /**
     * Ask the host to pin a new widget of [kind] for [base]/[quote] (pair
     * only for market/converter kinds). The pair is stashed as a pending
     * handoff AND sent as launch extras so the widget self-configures on
     * launchers that skip the config activity. No-op when [canPin] is false.
     */
    fun requestPin(kind: WidgetKind, base: String, quote: String)

    /** Open the host's configuration UI for widget instance [id]. */
    fun reconfigure(id: String)

    /** Trigger an immediate refresh of every placed widget. */
    fun refreshAll()

    /**
     * Apply the refresh policy (Account → Widgets "Realtime rates"):
     * realtime = 15-min periodic + foreground refreshes, default 30-min.
     */
    fun applyRefreshPolicy(realtime: Boolean)
}

/** Fallback used when no platform implementation is registered in Koin. */
object DefaultWidgetsHostBridge : WidgetsHostBridge {
    override val canPin: Boolean = false
    override val canManage: Boolean = false
    override fun requestPin(kind: WidgetKind, base: String, quote: String) = Unit
    override fun reconfigure(id: String) = Unit
    override fun refreshAll() = Unit

    override fun applyRefreshPolicy(realtime: Boolean) = Unit
}
