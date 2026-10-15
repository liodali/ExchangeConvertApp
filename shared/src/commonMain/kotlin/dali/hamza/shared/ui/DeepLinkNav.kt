package dali.hamza.shared.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Deep-link handoff from platform hosts into the shared navigation.
 * Hosts (widget taps via URL scheme / intents, notifications) stash a
 * pending link; [ExchangeCurrencyApp] reacts and navigates.
 *
 * Backed by Compose state so links delivered while the app is ALREADY
 * running (iOS onOpenURL on a warm app) still navigate — the old one-shot
 * LaunchedEffect(Unit) consumed cold-start links only.
 */
object DeepLinkNav {
    const val EXTRA_ROUTE = "dali.hamza.shared.route"
    const val EXTRA_PAIR_BASE = "dali.hamza.shared.pair.base"
    const val EXTRA_PAIR_QUOTE = "dali.hamza.shared.pair.quote"
    const val EXTRA_AMOUNT = "dali.hamza.shared.amount"

    data class PendingLink(
        val route: String,
        val base: String? = null,
        val quote: String? = null,
        val amount: String? = null,
    )

    var pending: PendingLink? by mutableStateOf(null)
        private set

    /** Swift/Android-callable stash (Kotlin Pair is awkward across bridges). */
    fun stash(
        route: String,
        pairBase: String? = null,
        pairQuote: String? = null,
        amount: String? = null,
    ) {
        pending = PendingLink(route, pairBase, pairQuote, amount)
    }

    /** Consumed by the NavHost after navigation. */
    fun clear() {
        pending = null
    }
}
