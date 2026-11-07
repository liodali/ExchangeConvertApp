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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-memory [IRepository] double for the converter's pair-rate fallback:
 * a base-quoted fiat table plus scripted on-demand pair quotes.
 */
private class FakeConverterRepository : IRepository {
    /** Base-quoted table rows (e.g. EUR → USD/TND). */
    var table: List<ExchangeRate> = emptyList()
    var base: String = "EUR"

    /** Scripted pair responses: "FROM:TO" → response. */
    val pairResponses = mutableMapOf<String, MyResponse<Double>>()
    val pairRequests = mutableListOf<String>()

    val recorded = mutableListOf<Transaction>()

    override suspend fun getLiveRateForPair(base: String, symbol: String): MyResponse<Double> {
        pairRequests += "$base:$symbol"
        return pairResponses["$base:$symbol"] ?: MyResponse.Empty
    }

    override suspend fun getListCurrencies(): MyResponse<List<Currency>> =
        MyResponse.Success(emptyList())

    override suspend fun syncCurrencyNames(lang: String): Boolean = false

    override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> =
        flow { emit(MyResponse.Success(emptyList())) }

    override suspend fun saveExchangeRatesOfCurrentCurrency() = Unit

    override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> =
        MyResponse.Success(table)

    override suspend fun getRatesForBase(base: String): MyResponse<List<ExchangeRate>> =
        MyResponse.Success(table)

    override suspend fun getCurrentCurrency(): String = base

    override suspend fun setCurrentCurrency(value: String) {
        base = value
    }

    override suspend fun refreshExchangeRates() = Unit

    override suspend fun getHistoricalRates(
        base: String,
        symbol: String,
        from: String,
        to: String,
    ): MyResponse<List<HistoricalRate>> = MyResponse.Success(emptyList())

    override suspend fun getHistoricalRates(
        base: String,
        symbols: List<String>,
        from: String,
        to: String,
    ): MyResponse<Map<String, List<HistoricalRate>>> = MyResponse.Success(emptyMap())

    override suspend fun getTransactions(): List<Transaction> = emptyList()

    override suspend fun recordTransaction(transaction: Transaction) {
        recorded += transaction
    }

    override suspend fun clearTransactions() = Unit

    override suspend fun getRateAlerts(): List<RateAlert> = emptyList()

    override suspend fun getEnabledRateAlerts(): List<RateAlert> = emptyList()

    override suspend fun addRateAlert(alert: RateAlert): Result<RateAlert> =
        Result.failure(IllegalStateException("unused"))

    override suspend fun removeRateAlert(id: Long) = Unit

    override suspend fun setRateAlertEnabled(id: Long, enabled: Boolean) = Unit

    override suspend fun updateRateAlertState(id: Long, lastRate: Double?, lastNotifiedAt: Long) = Unit

    override suspend fun getServerRateAlerts(): List<RateAlert> = emptyList()

    override suspend fun addServerRateAlert(alert: RateAlert): Result<RateAlert> =
        Result.failure(IllegalStateException("unused"))

    override suspend fun removeServerRateAlert(id: Long) = Unit

    override suspend fun setServerRateAlertEnabled(id: Long, enabled: Boolean) = Unit
}

/**
 * Converter cross-rate fallback for assets (plans/gold-section.md): metals
 * never ride in the base-quoted table, so XAU pairs resolve through
 * on-demand pair quotes (cached + inverted as needed) instead of showing 0.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelPairRateTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository() = FakeConverterRepository().apply {
        table = listOf(
            ExchangeRate(name = "USD", calculatedAmount = 1.08, rate = 1.08, time = 0L),
            ExchangeRate(name = "TND", calculatedAmount = 3.35, rate = 3.35, time = 0L),
        )
    }

    @Test
    fun fiatPairsStillConvertFromTheTableWithoutPairFetches() = runTest(dispatcher) {
        val fake = repository()
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("EUR", "Euro"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("100")
        advanceUntilIdle()

        assertEquals(335.0, vm.state.value.convertedAmount!!, 1e-9)
        assertTrue(fake.pairRequests.isEmpty(), "table-derivable pairs must not fetch")
    }

    @Test
    fun metalPairResolvesOnDemandAndConverts() = runTest(dispatcher) {
        val fake = repository()
        fake.pairResponses["XAU:TND"] = MyResponse.Success(11_599.05)
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("1")
        advanceUntilIdle()

        assertNotNull(vm.state.value.convertedAmount, "the bug: XAU pairs used to stay 0/null")
        assertEquals(11_599.05, vm.state.value.convertedAmount!!, 1e-9)
        assertEquals(listOf("XAU:TND"), fake.pairRequests)
    }

    @Test
    fun inverseMetalPairFetchesItsOwnDirection() = runTest(dispatcher) {
        val fake = repository()
        // the backend serves fiat→metal directly (TND→XAU ≈ 8.62e-5)
        fake.pairResponses["TND:XAU"] = MyResponse.Success(1.0 / 11_599.05)
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("TND", "Dinar"))
        vm.selectToCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.onAmountChange("11599.05")
        advanceUntilIdle()

        assertEquals(1.0, vm.state.value.convertedAmount!!, 1e-6)
        assertEquals(listOf("TND:XAU"), fake.pairRequests)
    }

    @Test
    fun swappedMetalPairReusesTheCachedDirection() = runTest(dispatcher) {
        val fake = repository()
        fake.pairResponses["XAU:TND"] = MyResponse.Success(11_599.05)
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("1")
        advanceUntilIdle()
        vm.swapCurrencies() // TND → XAU now
        vm.onAmountChange("11599.05")
        advanceUntilIdle()

        // the cached XAU:TND quote inverts — no second fetch
        assertEquals(listOf("XAU:TND"), fake.pairRequests)
        assertEquals(1.0, vm.state.value.convertedAmount!!, 1e-6)
    }

    @Test
    fun executeRecordsTheLedgerEntryAfterThePairSettles() = runTest(dispatcher) {
        val fake = repository()
        fake.pairResponses["XAU:TND"] = MyResponse.Success(11_599.05)
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("2")
        vm.convert()
        advanceUntilIdle()

        val tx = fake.recorded.singleOrNull()
        assertNotNull(tx, "Execute must record after the pair rate settles")
        assertEquals("XAU", tx.base)
        assertEquals("TND", tx.quote)
        assertEquals(2.0, tx.amountBase, 1e-9)
        assertEquals(23_198.10, tx.amountQuote, 1e-6)
        assertEquals(11_599.05, tx.rate, 1e-6)
    }

    @Test
    fun quotableNowhereMetalPairShowsNoResult() = runTest(dispatcher) {
        val fake = repository()
        // no pair scripted → Empty
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("1")
        advanceUntilIdle()

        assertNull(vm.state.value.convertedAmount, "no fabricated rate, no 0 — just no result")
    }

    @Test
    fun pairRateCacheServesSubsequentRecomputesWithoutRefetch() = runTest(dispatcher) {
        val fake = repository()
        fake.pairResponses["XAU:TND"] = MyResponse.Success(11_599.05)
        val vm = SharedViewModel(fake)
        advanceUntilIdle()

        vm.selectFromCurrency(Currency("XAU", "Gold (troy ounce)"))
        vm.selectToCurrency(Currency("TND", "Dinar"))
        vm.onAmountChange("1")
        advanceUntilIdle()
        vm.onAmountChange("2")
        vm.onAmountChange("3")
        advanceUntilIdle()

        assertEquals(1, fake.pairRequests.size, "keystrokes reuse the cached pair rate")
        assertEquals(3 * 11_599.05, vm.state.value.convertedAmount!!, 1e-6)
    }
}
