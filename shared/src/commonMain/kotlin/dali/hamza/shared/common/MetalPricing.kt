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

    /** Silver fineness rows (‰) the Metals detail displays, finest first. */
    val SILVER_PURITIES: List<Int> = listOf(999, 950, 925, 900)

    /**
     * Mass units for gold pricing display. `oz` is the **troy** ounce —
     * the gold market convention (31.1034768 g, not the 28.35 g avoirdupois
     * ounce); XAU catalog units are troy ounces too.
     */
    enum class GoldUnit(val label: String, val gramsPerUnit: Double) {
        GRAM("g", 1.0),
        TROY_OUNCE("oz", TROY_OUNCE_GRAMS),
        KILOGRAM("kg", 1000.0),
    }

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

    /** Per-karat values for [GOLD_KARATS] in one pass, rounded only by the
     *  caller when formatting. */
    fun goldKaratValues(spotPerTroyOunce: Double): List<Double> =
        GOLD_KARATS.map { karatPerGram(spotPerTroyOunce, it) }

    /**
     * Indicative value per gram of a silver [fineness] alloy — the
     * fine-silver fraction f/1000. Same no-premium rule as the karat math.
     */
    fun silverPurityPerGram(spotPerTroyOunce: Double, fineness: Int): Double =
        spotPerUnit(spotPerTroyOunce, GoldUnit.GRAM) * fineness / 1000.0

    /** Per-fineness values for [SILVER_PURITIES] in one pass. */
    fun silverPurityValues(spotPerTroyOunce: Double): List<Double> =
        SILVER_PURITIES.map { silverPurityPerGram(spotPerTroyOunce, it) }

    /** Indicative [karat] value for ONE [unit] (g / troy oz / kg). */
    fun karatValue(spotPerTroyOunce: Double, karat: Int, unit: GoldUnit): Double =
        karatPerGram(spotPerTroyOunce, karat) * unit.gramsPerUnit

    /** Fine-gold spot for ONE [unit]. `TROY_OUNCE` returns the spot itself. */
    fun spotPerUnit(spotPerTroyOunce: Double, unit: GoldUnit): Double =
        spotPerTroyOunce / TROY_OUNCE_GRAMS * unit.gramsPerUnit

    /**
     * [amount] of [unit] expressed in XAU catalog units (troy ounces) —
     * the converter handoff: `toTroyOunces(1.0, GRAM)` ≈ 0.032151.
     */
    fun toTroyOunces(amount: Double, unit: GoldUnit): Double =
        amount * unit.gramsPerUnit / TROY_OUNCE_GRAMS
}
