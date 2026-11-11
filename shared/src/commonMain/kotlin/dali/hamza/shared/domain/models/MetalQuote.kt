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
 * - [deltaPercent] is the day-Δ vs the previous COMPLETED session close
 *   (latest historic rate strictly before today, same currency), computed
 *   by [dali.hamza.shared.ui.viewmodel.MetalsViewModel]. Null when no close
 *   exists (weekend gaps, historic leg failed) — the chip renders an em
 *   dash rather than a fabricated change. Accepted basis skew: the historic
 *   waterfall may settle closes from futures legs while the live leg is
 *   spot, so the delta is directional guidance, not a settlement figure.
 */
data class MetalQuote(
    val metal: PreciousMetal,
    val currency: String,
    val spotPerTroyOunce: Double,
    val fetchedAtMs: Long,
    val deltaPercent: Double? = null,
)

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
