package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-memory [IRepository] double for the Gold section: pair quotes are
 * scripted per symbol and can be gated to orchestrate in-flight loads.
 */
private class FakeGoldRepository : IRepository {
    /** Scripted pair responses: "BASE:SYMBOL" → response. */
    val pairResponses = mutableMapOf<String, MyResponse<Double>>()
    val pairRequests = mutableListOf<String>()
    /** Optional gate per pair — the response waits until it completes. */
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

    var pairCallCount = 0
        private set

    override suspend fun getLiveRateForPair(base: String, symbol: String): MyResponse<Double> {
        pairCallCount += 1
        val key = "$base:$symbol"
        pairRequests += key
        gates[key]?.await()
        return pairResponses[key] ?: MyResponse.Empty
    }

    override suspend fun getListCurrencies(): MyResponse<List<Currency>> =
        MyResponse.Success(emptyList())

    override suspend fun syncCurrencyNames(lang: String): Boolean = false

    override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> =
        flow { emit(MyResponse.Success(emptyList())) }

    override suspend fun saveExchangeRatesOfCurrentCurrency() = Unit

    override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> =
        MyResponse.Success(emptyList())

    override suspend fun getRatesForBase(base: String): MyResponse<List<ExchangeRate>> =
        MyResponse.Success(emptyList())

    override suspend fun getCurrentCurrency(): String = "USD"

    override suspend fun setCurrentCurrency(value: String) = Unit

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

    override suspend fun recordTransaction(transaction: Transaction) = Unit

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
 * Gold section orchestration (plans/gold-section.md §5.3/§6): currency-keyed
 * loads, USD fallback for quotable-nowhere bases, stale-on-failure, race
 * discarding and request deduplication.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoldViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repo() = FakeGoldRepository()

    @Test
    fun directQuoteLoadsSnapshotInDisplayCurrency() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        val vm = GoldViewModel(repository)

        vm.load("TND")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("TND", state.snapshot?.currency)
        assertEquals(12_016.8, state.snapshot?.spotPerTroyOunce)
        assertFalse(state.loading)
        assertFalse(state.usdFallback)
        assertFalse(state.stale)
        assertFalse(state.unavailable)
    }

    @Test
    fun blankCurrencyLoadsUsd() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:USD"] = MyResponse.Success(3876.4)
        val vm = GoldViewModel(repository)

        vm.load("  ")
        advanceUntilIdle()

        assertEquals("USD", vm.state.value.snapshot?.currency)
        assertEquals(listOf("XAU:USD"), repository.pairRequests)
    }

    @Test
    fun quotableNowhereBaseFallsBackToLabeledUsd() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:USD"] = MyResponse.Success(3876.4)
        // XAU:XAU — an asset display base has no gold quote (Empty default)
        val vm = GoldViewModel(repository)

        vm.load("XAU")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("USD", state.snapshot?.currency, "fallback quote is USD")
        assertTrue(state.usdFallback, "fallback must be explicitly labeled")
        assertEquals("XAU", state.requestedCurrency)
        assertEquals(listOf("XAU:XAU", "XAU:USD"), repository.pairRequests)
    }

    @Test
    fun transportErrorNeverFallsBackToUsd() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Error("offline")
        val vm = GoldViewModel(repository)

        vm.load("TND")
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.snapshot, "an error must not mint USD data")
        assertFalse(state.usdFallback)
        assertTrue(state.unavailable, "no data at all → retry affordance")
        assertEquals(listOf("XAU:TND"), repository.pairRequests, "no USD masking call")
    }

    @Test
    fun failedRefreshKeepsLastValidSnapshotAsStale() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        val vm = GoldViewModel(repository)
        vm.load("TND")
        advanceUntilIdle()
        val first = vm.state.value.snapshot

        // refresh fails (transport error): the valid TND snapshot survives
        repository.pairResponses["XAU:TND"] = MyResponse.Error("offline")
        vm.load("TND", forceRefresh = true)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(first, state.snapshot, "last valid data is never cleared")
        assertTrue(state.stale)
        assertFalse(state.unavailable)
    }

    @Test
    fun outdatedResponseIsDiscardedOnCurrencySwitch() = runTest(dispatcher) {
        val repository = repo()
        val gate = CompletableDeferred<Unit>()
        repository.gates["XAU:TND"] = gate
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        repository.pairResponses["XAU:EUR"] = MyResponse.Success(3471.7)
        val vm = GoldViewModel(repository)

        vm.load("TND") // in flight, gated
        advanceUntilIdle()
        assertTrue(vm.state.value.loading)

        vm.load("EUR") // switch: generation bump cancels TND's relevance
        advanceUntilIdle()
        assertEquals("EUR", vm.state.value.snapshot?.currency)

        // the TND response completing late must NOT overwrite EUR data
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("EUR", vm.state.value.snapshot?.currency, "late response discarded")
    }

    @Test
    fun duplicateInFlightLoadsCoalesce() = runTest(dispatcher) {
        val repository = repo()
        val gate = CompletableDeferred<Unit>()
        repository.gates["XAU:TND"] = gate
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        val vm = GoldViewModel(repository)

        vm.load("TND")
        advanceUntilIdle()
        vm.load("TND") // same currency while in flight → no-op
        vm.load("TND")
        advanceUntilIdle()
        assertEquals(1, repository.pairCallCount)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("TND", vm.state.value.snapshot?.currency)
    }

    @Test
    fun freshDataIsNotRefetchedOnTabReturn() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        val vm = GoldViewModel(repository)
        vm.load("TND")
        advanceUntilIdle()

        vm.load("TND") // returning to Home with valid data
        advanceUntilIdle()
        assertEquals(1, repository.pairCallCount)
    }

    @Test
    fun retryForcesARefetch() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Error("offline")
        val vm = GoldViewModel(repository)
        vm.load("TND")
        advanceUntilIdle()
        assertTrue(vm.state.value.unavailable)

        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        vm.retry()
        advanceUntilIdle()

        assertEquals("TND", vm.state.value.snapshot?.currency)
        assertFalse(vm.state.value.unavailable)
    }

    @Test
    fun valuesAreNeverReusedUnderTheWrongCurrencyLabel() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Success(12_016.8)
        repository.pairResponses["XAU:EUR"] = MyResponse.Success(3471.7)
        val vm = GoldViewModel(repository)

        vm.load("TND")
        advanceUntilIdle()
        vm.load("EUR")
        advanceUntilIdle()

        val snapshot = vm.state.value.snapshot!!
        assertEquals("EUR", snapshot.currency)
        assertEquals(3471.7, snapshot.spotPerTroyOunce, "never TND's 12,016.8 relabeled EUR")
    }

    @Test
    fun invalidQuotesAreRejectedNotDisplayed() = runTest(dispatcher) {
        val repository = repo()
        repository.pairResponses["XAU:TND"] = MyResponse.Success(0.0)
        repository.pairResponses["XAU:USD"] = MyResponse.Success(3876.4)
        val vm = GoldViewModel(repository)

        vm.load("TND")
        advanceUntilIdle()

        // 0.0 is invalid → not shown as 0.00; USD fallback supplies data
        val state = vm.state.value
        assertEquals("USD", state.snapshot?.currency)
        assertTrue(state.usdFallback)
    }
}
