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
}
