package dali.hamza.shared.common

import dali.hamza.shared.ui.components.formatGoldAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Gold unit/purity arithmetic (plans/gold-section.md §4):
 * `karatPerGram(spot, k) = spot / 31.1034768 * k / 24`, rounded only when
 * formatting — no intermediate rounding anywhere.
 *
 * Design fixtures (§2) verified: at USD 2,342.10/troy oz the exact math
 * gives 75.300264/69.025242/56.475198/43.925154 for 24K/22K/18K/14K,
 * which formats to the design's 75.30 / 69.03 / 56.48 / 43.93 USD/g.
 */
class MetalPricingTest {

    private val spot = 2342.10 // USD per troy oz (design fixture)

    @Test
    fun fineGoldPerGramMatchesDesignFixture() {
        assertEquals(75.300264, MetalPricing.fineGoldPerGram(spot), absoluteTolerance = 1e-6)
        // formatted display rounds ONCE at the boundary: 75.30
        assertEquals("75.30", formatGoldAmount(MetalPricing.fineGoldPerGram(spot)))
    }

    @Test
    fun karatFractionsAreExactWithNoIntermediateRounding() {
        assertEquals(75.300264, MetalPricing.karatPerGram(spot, 24), absoluteTolerance = 1e-6)
        assertEquals(69.025242, MetalPricing.karatPerGram(spot, 22), absoluteTolerance = 1e-6)
        assertEquals(56.475198, MetalPricing.karatPerGram(spot, 18), absoluteTolerance = 1e-6)
        assertEquals(43.925154, MetalPricing.karatPerGram(spot, 14), absoluteTolerance = 1e-6)
        // the design's displayed quartet, straight from exact math
        assertEquals(
            listOf("75.30", "69.03", "56.48", "43.93"),
            MetalPricing.goldKaratValues(spot).map(::formatGoldAmount),
        )
    }

    @Test
    fun karatSeriesMatchesTheKaratOrdering() {
        val values = MetalPricing.goldKaratValues(spot)
        assertEquals(MetalPricing.GOLD_KARATS.size, values.size)
        MetalPricing.GOLD_KARATS.indices.forEach { i ->
            assertEquals(
                MetalPricing.karatPerGram(spot, MetalPricing.GOLD_KARATS[i]),
                values[i],
                absoluteTolerance = 0.0,
            )
        }
    }

    @Test
    fun spotValidationRejectsMissingZeroNegativeAndNonFinite() {
        assertTrue(MetalPricing.isValidSpot(3876.40))
        assertTrue(MetalPricing.isValidSpot(0.0001))
        assertFalse(MetalPricing.isValidSpot(0.0), "zero must never display as 0.00")
        assertFalse(MetalPricing.isValidSpot(-3876.40))
        assertFalse(MetalPricing.isValidSpot(Double.NaN))
        assertFalse(MetalPricing.isValidSpot(Double.POSITIVE_INFINITY))
    }

    @Test
    fun amountFormattingHandlesHighAndLowPricesWithGrouping() {
        // spot-scale values (integer grouping, design "2,342.10")
        assertEquals("2,342.10", formatGoldAmount(2342.1))
        assertEquals("3,876.40", formatGoldAmount(3876.399902))
        // gram-scale values
        assertEquals("75.30", formatGoldAmount(75.298790))
        assertEquals("43.92", formatGoldAmount(43.924294))
        // high-price currencies (JPY per gram is six digits)
        assertEquals("590,123.45", formatGoldAmount(590123.451))
        // low-price currencies keep two fraction digits
        assertEquals("0.05", formatGoldAmount(0.05))
        assertEquals("1.00", formatGoldAmount(1.0))
    }

    @Test
    fun formattingRoundsOnceAtTheBoundary() {
        // 75.295 → 75.30 (round half away from zero, single rounding)
        assertEquals("75.30", formatGoldAmount(75.295))
        // 75.2949 → 75.29 — intermediate rounding would give 75.30
        assertEquals("75.29", formatGoldAmount(75.2949))
    }

    // ---- unit selection (g | troy oz | kg) -------------------------------

    @Test
    fun perUnitValuesScaleExactlyByGramsPerUnit() {
        val oz = MetalPricing.karatValue(spot, 24, MetalPricing.GoldUnit.TROY_OUNCE)
        assertEquals(spot, oz, absoluteTolerance = 1e-9, message = "24K per troy oz is the spot itself")
        val gram = MetalPricing.karatValue(spot, 24, MetalPricing.GoldUnit.GRAM)
        assertEquals(75.300264, gram, absoluteTolerance = 1e-6)
        val kilogram = MetalPricing.karatValue(spot, 24, MetalPricing.GoldUnit.KILOGRAM)
        assertEquals(gram * 1000.0, kilogram, absoluteTolerance = 1e-9)
        // karat fraction holds in every unit: 22K is 11/12 of 24K
        assertEquals(
            MetalPricing.karatValue(spot, 24, MetalPricing.GoldUnit.KILOGRAM) * 11.0 / 12.0,
            MetalPricing.karatValue(spot, 22, MetalPricing.GoldUnit.KILOGRAM),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun spotPerUnitCoversAllThreeUnits() {
        val units = listOf(
            MetalPricing.GoldUnit.GRAM to 75.300264,
            MetalPricing.GoldUnit.TROY_OUNCE to spot,
            MetalPricing.GoldUnit.KILOGRAM to 75_300.2635,
        )
        units.forEach { (unit, expected) ->
            assertEquals(expected, MetalPricing.spotPerUnit(spot, unit), absoluteTolerance = 1e-3)
        }
    }

    @Test
    fun troyOunceHandoffConvertsEachUnitExactly() {
        assertEquals(1.0, MetalPricing.toTroyOunces(1.0, MetalPricing.GoldUnit.TROY_OUNCE), absoluteTolerance = 1e-12)
        assertEquals(0.032151, MetalPricing.toTroyOunces(1.0, MetalPricing.GoldUnit.GRAM), absoluteTolerance = 1e-6)
        assertEquals(32.150747, MetalPricing.toTroyOunces(1.0, MetalPricing.GoldUnit.KILOGRAM), absoluteTolerance = 1e-6)
        // 5 troy ounces in grams: 5 × 31.1034768
        assertEquals(
            5.0 * MetalPricing.TROY_OUNCE_GRAMS,
            MetalPricing.toTroyOunces(5.0, MetalPricing.GoldUnit.TROY_OUNCE) * MetalPricing.TROY_OUNCE_GRAMS,
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun unitLabelsAreShortUniversalSymbols() {
        assertEquals(listOf("g", "oz", "kg"), MetalPricing.GoldUnit.entries.map { it.label })
    }
}
