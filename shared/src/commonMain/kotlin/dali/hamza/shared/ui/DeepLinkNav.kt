package dali.hamza.shared.ui

import kotlin.concurrent.Volatile

/**
 * One-shot deep-link handoff from platform hosts into the shared
 * navigation: the Android host (widget intents, notifications) puts a
 * route extra on the launch Intent; [ExchangeCurrencyApp] consumes it
 * once after the NavHost is ready and clears it.
 */
object DeepLinkNav {
    const val EXTRA_ROUTE = "dali.hamza.shared.route"
    const val EXTRA_PAIR_BASE = "dali.hamza.shared.pair.base"
    const val EXTRA_PAIR_QUOTE = "dali.hamza.shared.pair.quote"

    @Volatile
    var pendingRoute: String? = null

    /** Pair carried by the route (history deep links from widgets). */
    @Volatile
    var pendingPair: Pair<String, String>? = null

    /** Swift-callable stash (Kotlin Pair is awkward across the bridge). */
    fun stash(route: String, pairBase: String? = null, pairQuote: String? = null) {
        pendingRoute = route
        pendingPair = if (pairBase != null && pairQuote != null) pairBase to pairQuote else null
    }
}
