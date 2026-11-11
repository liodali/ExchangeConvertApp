package dali.hamza.shared.domain.models

/**
 * One precious-metal spot quote (metals v3 — plans/metals-v3.md §5).
 *
 * Mirrors [GoldSnapshot]'s semantics so the two sections stay consistent:
 * - [currency] is always a fiat display currency — a non-quotable base falls
 *   back to USD and the UI labels it as such;
 * - [spotPerTroyOunce] is spot in [currency] per 1 troy ounce (the
 *   `/latest?base=CODE&symbol=CUR` contract); per-gram values are derived
 *   via [dali.hamza.shared.common.MetalPricing] — never stored rounded;
 * - deltas follow Gold's comparable-close policy: the historic waterfall
 *   can mix spot and futures closes, so [deltaPercent] stays null and the
 *   chip renders an em dash instead of a fabricated change.
 */
data class MetalQuote(
    val metal: PreciousMetal,
    val currency: String,
    val spotPerTroyOunce: Double,
    val fetchedAtMs: Long,
) {
    /** Percent change vs a comparable close — null by policy (see class doc). */
    val deltaPercent: Double? get() = null
}

/**
 * The four troy-ounce metals the v3 overview serves. Order matches the
 * design grid (`SEoKM`): Gold, Silver, Platinum, Palladium.
 */
enum class PreciousMetal(val code: String) {
    GOLD("XAU"),
    SILVER("XAG"),
    PLATINUM("XPT"),
    PALLADIUM("XPD");

    companion object {
        fun byCode(code: String): PreciousMetal? = entries.firstOrNull { it.code == code }
    }
}
