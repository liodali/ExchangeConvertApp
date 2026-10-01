package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.domain.models.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryMathTest {

    private fun tx(
        base: String,
        quote: String,
        amountBase: Double,
        amountQuote: Double,
    ) = Transaction(
        base = base,
        quote = quote,
        amountBase = amountBase,
        amountQuote = amountQuote,
        rate = amountQuote / amountBase,
        timestamp = 0L,
    )

    @Test
    fun balancesAddQuoteAndSubtractBase() {
        val balances = HistoryViewModel.computeBalances(
            listOf(
                tx("EUR", "USD", 500.0, 560.0),
                tx("EUR", "USD", 200.0, 225.0),
                tx("GBP", "EUR", 100.0, 115.0),
            )
        ).associateBy { it.currency }

        // EUR: -500 -200 from the two exchanges, +115 received from GBP = -585
        assertEquals(-585.0, balances["EUR"]!!.balance)
        assertEquals(785.0, balances["USD"]!!.balance)
        assertEquals(-100.0, balances["GBP"]!!.balance)
    }

    @Test
    fun balancesSortedByAbsoluteValue() {
        val balances = HistoryViewModel.computeBalances(
            listOf(
                tx("EUR", "USD", 500.0, 560.0),
                tx("USD", "GBP", 2000.0, 1550.0),
            )
        )
        // GBP +1550 | USD -1440 | EUR -500
        assertEquals(listOf("GBP", "USD", "EUR"), balances.map { it.currency })
    }

    @Test
    fun emptyLedgerHasNoBalances() {
        assertEquals(emptyList(), HistoryViewModel.computeBalances(emptyList()))
    }

    @Test
    fun percentileRankCountsValuesBelow() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        // 4 of 5 values are below 5.0 → top 20%
        assertEquals(20, HistoryViewModel.percentileRank(values, 5.0))
        // nothing below the minimum → top 100%
        assertEquals(100, HistoryViewModel.percentileRank(values, 1.0))
        // everything below the maximum minus one
        assertEquals(40, HistoryViewModel.percentileRank(values, 4.0))
    }

    @Test
    fun percentileRankOfEmptySeriesIsNull() {
        assertNull(HistoryViewModel.percentileRank(emptyList(), 1.0))
    }
}
