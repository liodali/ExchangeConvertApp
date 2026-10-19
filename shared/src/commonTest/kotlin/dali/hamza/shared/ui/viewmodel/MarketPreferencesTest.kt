package dali.hamza.shared.ui.viewmodel

import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.domain.models.ExchangeRate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketPreferencesTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun rate(code: String, value: Double) = ExchangeRate(code, value, value, 0L)

    @Test
    fun quickExchangeSelectionSwapAndConversionPreserveMarketBase() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            liveRates = listOf(rate("USD", 1.2), rate("GBP", 0.8))
        }
        val viewModel = SharedViewModel(repository)
        advanceUntilIdle()

        viewModel.selectFromCurrency(CurrenciesCatalog.byCode("GBP")!!)
        viewModel.selectToCurrency(CurrenciesCatalog.byCode("USD")!!)
        viewModel.onAmountChange("10")
        assertEquals(15.0, viewModel.state.value.convertedAmount!!, 1e-9)
        assertEquals(1.5, viewModel.currentRate()!!, 1e-9)

        viewModel.swapCurrencies()
        viewModel.convert()
        advanceUntilIdle()
        assertEquals(10.0 / 1.5, viewModel.state.value.convertedAmount!!, 1e-9)
        assertEquals("EUR", viewModel.state.value.marketBase)
        assertEquals("EUR", viewModel.state.value.ratesBase)
        assertEquals("EUR", repository.currentCurrency)
        assertTrue(repository.baseChanges.isEmpty())
        assertEquals("USD", repository.recorded.single().base)

        viewModel.selectToCurrency(CurrenciesCatalog.byCode("EUR")!!)
        assertEquals(1.0 / 1.2, viewModel.currentRate()!!, 1e-9)
    }

    @Test
    fun accountBasePersistsAndReloadsHomeWithoutChangingQuickExchangePair() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            ratesByBase = mapOf(
                "EUR" to listOf(rate("USD", 1.2), rate("GBP", 0.8)),
                "USD" to listOf(rate("EUR", 1.0 / 1.2), rate("GBP", 0.8 / 1.2)),
            )
        }
        val viewModel = SharedViewModel(repository)
        advanceUntilIdle()
        viewModel.selectFromCurrency(CurrenciesCatalog.byCode("GBP")!!)
        viewModel.selectToCurrency(CurrenciesCatalog.byCode("EUR")!!)
        viewModel.onAmountChange("10")

        viewModel.selectMarketBase("USD")
        advanceUntilIdle()

        assertEquals(listOf("USD"), repository.baseChanges)
        assertEquals("USD", viewModel.state.value.marketBase)
        assertEquals("USD", viewModel.state.value.ratesBase)
        assertEquals(repository.ratesByBase["USD"], viewModel.state.value.rates)
        assertEquals("GBP", viewModel.state.value.fromCurrency?.name)
        assertEquals("EUR", viewModel.state.value.toCurrency?.name)
        assertEquals(12.5, viewModel.state.value.convertedAmount!!, 1e-9)

        val restarted = SharedViewModel(repository)
        advanceUntilIdle()
        assertEquals("USD", restarted.state.value.marketBase)
    }

    @Test
    fun newMarketSelectionCannotBeOverwrittenByOldHistoryRequest() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository().apply { historicalGate = gate }
        val viewModel = HomeViewModel(repository)
        viewModel.load("EUR", listOf(rate("USD", 1.2)))
        advanceUntilIdle()

        repository.historicalGate = null
        viewModel.load("EUR", listOf(rate("GBP", 0.8)))
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("GBP"), viewModel.state.value.pairCards.map { it.quote })
        viewModel.load("USD", emptyList())
        assertTrue(viewModel.state.value.pairCards.isEmpty())
    }
}
