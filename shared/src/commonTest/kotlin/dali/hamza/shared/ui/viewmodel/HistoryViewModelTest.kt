package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * In-memory [IRepository] double for History cluster tests.
 */
private class FakeRepository : IRepository {

    val recorded = mutableListOf<Transaction>()
    val historicalRequests = mutableListOf<List<String>>() // base, symbol, from, to

    var liveRates: List<ExchangeRate> = emptyList()
    var series: List<HistoricalRate> = emptyList()
    var transactions: List<Transaction> = emptyList()

    override suspend fun getListCurrencies(): MyResponse<List<Currency>> =
        MyResponse.Success(emptyList())

    override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> =
        flow { emit(MyResponse.Success(emptyList())) }

    override suspend fun saveExchangeRatesOfCurrentCurrency() = Unit

    override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> =
        MyResponse.Success(liveRates)

    override suspend fun getCurrentCurrency(): String = "EUR"

    override suspend fun setCurrentCurrency(value: String) = Unit

    override suspend fun refreshExchangeRates() = Unit

    override suspend fun getHistoricalRates(
        base: String,
        symbol: String,
        from: String,
        to: String,
    ): MyResponse<List<HistoricalRate>> {
        historicalRequests.add(listOf(base, symbol, from, to))
        return if (series.isEmpty()) {
            MyResponse.Error("no data")
        } else {
            MyResponse.Success(series)
        }
    }

    override suspend fun getHistoricalRates(
        base: String,
        symbols: List<String>,
        from: String,
        to: String,
    ): MyResponse<Map<String, List<HistoricalRate>>> =
        MyResponse.Success(emptyMap())

    override suspend fun getTransactions(): List<Transaction> = transactions

    override suspend fun recordTransaction(transaction: Transaction) {
        recorded.add(transaction)
    }

    override suspend fun clearTransactions() {
        recorded.clear()
    }

    // ---- rate alerts (local notifications) -----------------------------

    val rateAlerts = mutableListOf<RateAlert>()
    private var nextAlertId = 1L

    override suspend fun getRateAlerts(): List<RateAlert> = rateAlerts.toList()

    override suspend fun getEnabledRateAlerts(): List<RateAlert> =
        rateAlerts.filter { it.enabled }

    override suspend fun addRateAlert(alert: RateAlert): Result<RateAlert> {
        if (rateAlerts.any { it.base == alert.base && it.quote == alert.quote }) {
            return Result.failure(IllegalStateException(RateAlert.DUPLICATE_MESSAGE))
        }
        if (rateAlerts.size >= RateAlert.maxAlertsForTier(dali.hamza.shared.domain.models.DataTier.GUEST)) {
            return Result.failure(IllegalStateException(RateAlert.LIMIT_MESSAGE))
        }
        val stored = alert.copy(id = nextAlertId++)
        rateAlerts += stored
        return Result.success(stored)
    }

    override suspend fun removeRateAlert(id: Long) {
        rateAlerts.removeAll { it.id == id }
    }

    override suspend fun setRateAlertEnabled(id: Long, enabled: Boolean) {
        val index = rateAlerts.indexOfFirst { it.id == id }
        if (index >= 0) rateAlerts[index] = rateAlerts[index].copy(enabled = enabled)
    }

    override suspend fun updateRateAlertState(id: Long, lastRate: Double?, lastNotifiedAt: Long) {
        val index = rateAlerts.indexOfFirst { it.id == id }
        if (index >= 0) {
            rateAlerts[index] = rateAlerts[index].copy(
                lastRate = lastRate,
                lastNotifiedAt = lastNotifiedAt,
            )
        }
    }

    // server-push alerts (Phase 2) — unused by History cluster tests
    override suspend fun getServerRateAlerts(): List<RateAlert> = emptyList()

    override suspend fun addServerRateAlert(alert: RateAlert): Result<RateAlert> =
        Result.failure(IllegalStateException("unused"))

    override suspend fun removeServerRateAlert(id: Long) = Unit

    override suspend fun setServerRateAlertEnabled(id: Long, enabled: Boolean) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repositoryWithData() = FakeRepository().apply {
        liveRates = listOf(ExchangeRate(name = "USD", rate = 1.20, calculatedAmount = 1.20, time = 0L))
        series = listOf(
            HistoricalRate("2026-09-25", 1.00),
            HistoricalRate("2026-09-26", 1.10),
            HistoricalRate("2026-09-27", 1.20),
        )
        transactions = listOf(
            Transaction(
                id = 1,
                base = "EUR",
                quote = "USD",
                amountBase = 100.0,
                amountQuote = 110.0,
                rate = 1.10,
                timestamp = 1_000L,
            ),
            Transaction(
                id = 2,
                base = "GBP",
                quote = "JPY",
                amountBase = 50.0,
                amountQuote = 9_000.0,
                rate = 180.0,
                timestamp = 2_000L,
            ),
        )
    }

    @Test
    fun loadBuildsFullState() = runTest(dispatcher) {
        val repository = repositoryWithData()
        val viewModel = HistoryViewModel(repository)

        viewModel.load("EUR", "USD")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("EUR", state.base)
        assertEquals("USD", state.quote)
        // live rate preferred over the series' last point
        assertEquals(1.20, state.currentRate)
        // series: 1.00 → 1.20 = +20%
        assertEquals(20.0, state.changePercent!!, 1e-9)
        assertEquals(0.20, state.delta!!, 1e-9)
        // best from the stored ledger (1.10), not the series max (1.20)
        assertEquals(1.10, state.bestRate)
        assertEquals(1_000L, state.bestTimestamp)
        // only the EUR/USD pair is listed
        assertEquals(listOf(1L), state.transactions.map { it.id })
        // balances: EUR -100, USD +110
        assertEquals(-100.0, state.balances.first { it.currency == "EUR" }.balance)
        assertEquals(110.0, state.balances.first { it.currency == "USD" }.balance)
        // top percentile: 1.20 is above 2 of 3 series points → top 34%
        assertEquals(34, state.topPercentile)
    }

    @Test
    fun loadRejectsIncompletePair() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(FakeRepository())

        viewModel.load(null, "USD")
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun selectRangeRefetchesSeries() = runTest(dispatcher) {
        val repository = repositoryWithData()
        val viewModel = HistoryViewModel(repository)

        viewModel.load("EUR", "USD")
        advanceUntilIdle()
        viewModel.selectRange(HistoryRange.ONE_YEAR)
        advanceUntilIdle()

        assertEquals(HistoryRange.ONE_YEAR, viewModel.state.value.range)
        assertEquals(2, repository.historicalRequests.size)
        // second request asked for a ~365-day window
        val second = repository.historicalRequests.last()
        val fromDay = dali.hamza.shared.common.DateUtils.epochDayOf(second[2])!!
        val toDay = dali.hamza.shared.common.DateUtils.epochDayOf(second[3])!!
        assertTrue(toDay - fromDay >= 364, "expected a 1Y window, got ${second[2]}..${second[3]}")
    }

    @Test
    fun seriesFallbackProvidesBestRateWhenLedgerIsEmpty() = runTest(dispatcher) {
        val repository = repositoryWithData()
        repository.transactions = emptyList()
        val viewModel = HistoryViewModel(repository)

        viewModel.load("EUR", "USD")
        advanceUntilIdle()

        // no stored transactions → the series max becomes the best rate
        assertEquals(1.20, viewModel.state.value.bestRate)
        assertEquals(null, viewModel.state.value.bestTimestamp)
    }
}
