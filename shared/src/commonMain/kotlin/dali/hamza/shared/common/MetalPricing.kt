package dali.hamza.shared.common

/**
 * Precious-metal unit and purity arithmetic (plans/gold-section.md §4).
 *
 * Pure functions only — no rounding happens here; formatting rounds at the
 * display boundary. Karat values are theoretical fine-gold-content values:
 * `karatPerGram(spot, k) = spot / TROY_OUNCE_GRAMS * k / 24`.
 */
object MetalPricing {

    /** Grams in one troy ounce — exact international definition. */
    const val TROY_OUNCE_GRAMS: Double = 31.1034768

    /** Karat purities the Gold section displays, finest first. */
    val GOLD_KARATS: List<Int> = listOf(24, 22, 18, 14)

    /**
     * A spot price fit for display: strictly positive and finite. Zero,
     * negative, NaN and infinite values must never render as 0.00.
     */
    fun isValidSpot(value: Double): Boolean = value > 0.0 && value.isFinite()

    /** Fine-gold (24K) value per gram from a per-troy-ounce [spot] quote. */
    fun fineGoldPerGram(spotPerTroyOunce: Double): Double =
        spotPerTroyOunce / TROY_OUNCE_GRAMS

    /**
     * Indicative value per gram of a [karat] alloy — the fine-gold fraction
     * k/24. Excludes premiums, making charges, tax and dealer spreads.
     */
    fun karatPerGram(spotPerTroyOunce: Double, karat: Int): Double =
        fineGoldPerGram(spotPerTroyOunce) * karat / 24.0

    /**
     * Per-karat values for [GOLD_KARATS] in one pass, rounded only by the
     * caller when formatting.
     */
    fun goldKaratValues(spotPerTroyOunce: Double): List<Double> =
        GOLD_KARATS.map { karatPerGram(spotPerTroyOunce, it) }
}
