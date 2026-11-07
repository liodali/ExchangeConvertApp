package dali.hamza.shared.domain.models

/**
 * One verified gold spot quote with its derived per-karat context
 * (plans/gold-section.md §5.1).
 *
 * - [currency] is always a **fiat** display currency — never an asset code;
 *   a non-quotable base falls back to USD and is labeled as such by the UI.
 * - [spotPerTroyOunce] is fine-gold spot in [currency] per 1 troy ounce
 *   (the `/latest?base=XAU&symbol=CUR` contract). Karat per-gram values are
 *   derived on demand via [dali.hamza.shared.common.MetalPricing] — never
 *   stored rounded.
 * - [fetchedAtMs] is the app-side fetch clock. The backend's `time` field is
 *   request time for metals, so the UI must say "Fetched at", never
 *   "Market price at".
 * - [previousClose] is the latest comparable earlier daily close in the same
 *   currency, unit and instrument. The historic waterfall can mix spot and
 *   futures quotes, so provenance is not currently establishable and the
 *   field stays null — [deltaPercent] then reads null and the UI shows an
 *   em dash instead of a fabricated change.
 */
data class GoldSnapshot(
    val currency: String,
    val spotPerTroyOunce: Double,
    val fetchedAtMs: Long,
    val previousClose: Double? = null,
) {
    /** Percent change vs [previousClose], or null when not comparable. */
    val deltaPercent: Double?
        get() = previousClose
            ?.takeIf { it > 0.0 && it.isFinite() }
            ?.let { (spotPerTroyOunce - it) / it * 100.0 }
}
