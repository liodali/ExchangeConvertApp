package dali.hamza.shared.domain.models

import kotlinx.serialization.Serializable

/**
 * Widget types — one entry per picker look. The stored [WidgetConfig.type]
 * is set by the widget itself at self-configuration; old configs carrying
 * the pre-split `MARKET_RATE` value coerce back to [MARKET_CARD]
 * (`coerceInputValues` in the store's Json).
 */
enum class WidgetType {
    MARKET_CARD,
    MARKET_WAVE,
    MARKET_GRID,
    QUICK_CONVERT,
}

/**
 * User configuration for one widget instance.
 *
 * [id] is the host's instance id — Android `appWidgetId` (as string);
 * iOS instances get a UUID when the WidgetKit phase lands.
 */
@Serializable
data class WidgetConfig(
    val id: String,
    val type: WidgetType = WidgetType.MARKET_CARD,
    val base: String,
    val quote: String,
    /**
     * Forgotten from the app: the placed instance renders a dead "Removed"
     * state (providers cannot unplace widgets — the launcher must remove
     * them) and the management list hides it.
     */
    val removed: Boolean = false,
    /**
     * MARKET_GRID only: custom pairs as "BASE/QUOTE" strings (2–3). Empty
     * means "follow the Account market preferences" (the default grid).
     */
    val pairs: List<String> = emptyList(),
) {
    val pair: String get() = "$base/$quote"

    /** Grid pairs — custom when present, otherwise defaults to preferences. */
    val gridPairs: List<Pair<String, String>>
        get() = pairs.mapNotNull { raw ->
            raw.split("/").takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }
}

/**
 * Latest fetched data for one pair — what a widget renders.
 * [prevClose] is the previous session close (day-Δ base); `null` when the
 * historic lookup failed (widget hides the Δ chip).
 */
@Serializable
data class WidgetSnapshot(
    val base: String,
    val quote: String,
    val rate: Double,
    val prevClose: Double? = null,
    val fetchedAt: Long,
) {
    /** Day change in percent; `null` without a [prevClose] baseline. */
    val dayDeltaPercent: Double?
        get() = prevClose?.takeIf { it > 0.0 }?.let { (rate - it) / it * 100.0 }
}
