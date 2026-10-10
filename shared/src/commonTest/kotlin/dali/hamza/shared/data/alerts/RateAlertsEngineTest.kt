package dali.hamza.shared.data.alerts

import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.LocalNotifier
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
import kotlin.test.assertTrue

/**
 * [RateAlertsEngine] unit tests — a controllable clock, an in-memory
 * repository/storage/notifier and a scripted rates fetcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RateAlertsEngineTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- fakes -----------------------------------------------------------

    private class FakeStorage : ISessionStorage {
        var masterSwitch = true
        override fun getCurrency(): String = "USD"
        override fun setCurrency(value: String) = Unit
        override fun getLastUpdate(): Long = 0L
        override fun setLastUpdate(timestamp: Long) = Unit
        override fun setUsername(value: String) = Unit
        override fun getNotificationsEnabled(): Boolean = masterSwitch
        override fun setNotificationsEnabled(enabled: Boolean) {
            masterSwitch = enabled
        }
    }

    private class FakeNotifier : LocalNotifier {
        val posted = mutableListOf<Pair<String, String>>()
        var enabled = true
        override fun areNotificationsEnabled(): Boolean = enabled
        override fun requestPermission(onResult: (Boolean) -> Unit) = onResult(enabled)
        override fun notify(id: Long, title: String, body: String) {
            posted += title to body
        }
    }

    private class FakeRepository : IRepository {
        val alerts = mutableListOf<RateAlert>()
        private var nextId = 1L

        fun seed(alert: RateAlert): RateAlert =
            alert.copy(id = nextId++).also { alerts += it }

        override suspend fun getListCurrencies(): MyResponse<List<Currency>> =
            MyResponse.Success(emptyList())

        override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> =
            flow { emit(MyResponse.Success(emptyList())) }

        override suspend fun saveExchangeRatesOfCurrentCurrency() = Unit

        override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> =
            MyResponse.Success(emptyList())

        override suspend fun getCurrentCurrency(): String = "USD"

        override suspend fun setCurrentCurrency(value: String) = Unit

        override suspend fun refreshExchangeRates() = Unit

        override suspend fun getHistoricalRates(
            base: String,
            symbol: String,
            from: String,
            to: String,
        ): MyResponse<List<HistoricalRate>> = MyResponse.Error("unused")

        override suspend fun getHistoricalRates(
            base: String,
            symbols: List<String>,
            from: String,
            to: String,
        ): MyResponse<Map<String, List<HistoricalRate>>> = MyResponse.Success(emptyMap())

        override suspend fun getTransactions(): List<Transaction> = emptyList()

        override suspend fun recordTransaction(transaction: Transaction) = Unit

        override suspend fun clearTransactions() = Unit

        override suspend fun getRateAlerts(): List<RateAlert> = alerts.toList()

        override suspend fun getEnabledRateAlerts(): List<RateAlert> = alerts.filter { it.enabled }

        override suspend fun addRateAlert(alert: RateAlert): Result<RateAlert> {
            val stored = alert.copy(id = nextId++)
            alerts += stored
            return Result.success(stored)
        }

        override suspend fun removeRateAlert(id: Long) {
            alerts.removeAll { it.id == id }
        }

        override suspend fun setRateAlertEnabled(id: Long, enabled: Boolean) {
            val index = alerts.indexOfFirst { it.id == id }
            if (index >= 0) alerts[index] = alerts[index].copy(enabled = enabled)
        }

        override suspend fun updateRateAlertState(
            id: Long,
            lastRate: Double?,
            lastNotifiedAt: Long,
        ) {
            val index = alerts.indexOfFirst { it.id == id }
            if (index >= 0) {
                alerts[index] = alerts[index].copy(
                    lastRate = lastRate,
                    lastNotifiedAt = lastNotifiedAt,
                )
            }
        }

        // server-push alerts (Phase 2) — unused by the local engine
        override suspend fun getServerRateAlerts(): List<RateAlert> = emptyList()

        override suspend fun addServerRateAlert(alert: RateAlert): Result<RateAlert> =
            Result.failure(IllegalStateException("unused"))

        override suspend fun removeServerRateAlert(id: Long) = Unit

        override suspend fun setServerRateAlertEnabled(id: Long, enabled: Boolean) = Unit
    }

    private class Fixture(
        var now: Long = 1_000_000L,
        rates: Map<String, Double>? = mapOf("USD" to 1.0),
    ) {
        val repository = FakeRepository()
        val storage = FakeStorage()
        val notifier = FakeNotifier()
        var currentRates = rates
        val engine = RateAlertsEngine(
            repository = repository,
            currencyApi = dummyApi,
            sessionStorage = storage,
            notifier = notifier,
            timeProvider = { now },
            fetchRates = { currentRates },
        )

        suspend fun check(force: Boolean = true) = engine.runCheck(force)
    }

    // ---- tests -----------------------------------------------------------

    @Test
    fun firstCheckOnlySeedsTheBaseline() = runTest(dispatcher) {
        val fixture = Fixture()
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.THRESHOLD,
                thresholdPercent = 0.5,
                lastNotifiedAt = 0L,
            )
        )

        val triggered = fixture.check()

        assertTrue(triggered.isEmpty(), "no previous rate → nothing to compare")
        assertEquals(1.0, fixture.repository.alerts.single().lastRate)
        assertTrue(fixture.notifier.posted.isEmpty())
    }

    @Test
    fun thresholdAlertFiresOnMoveAndRebases() = runTest(dispatcher) {
        val fixture = Fixture(rates = mapOf("USD" to 1.02))
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.THRESHOLD,
                thresholdPercent = 0.5,
                lastRate = 1.00,
                lastNotifiedAt = 0L,
            )
        )

        val triggered = fixture.check()
        advanceUntilIdle()

        assertEquals(1, triggered.size)
        assertEquals(1, fixture.notifier.posted.size)
        val stored = fixture.repository.alerts.single()
        assertEquals(1.02, stored.lastRate)             // rebased baseline
        assertEquals(fixture.now, stored.lastNotifiedAt)

        // same rate again → no repeated notification
        fixture.now += 30 * 60_000L
        val second = fixture.check()
        assertTrue(second.isEmpty())
        assertEquals(1, fixture.notifier.posted.size)
    }

    @Test
    fun periodicAlertWaitsForItsInterval() = runTest(dispatcher) {
        val fixture = Fixture(rates = mapOf("USD" to 1.00))
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.PERIODIC,
                intervalMinutes = RateAlert.INTERVAL_TWO_HOURS,
                lastRate = 1.00,
                lastNotifiedAt = fixture.now, // notified "just now"
            )
        )

        // 90 minutes later — past 1h, not yet past the 2h interval
        fixture.now += 90 * 60_000L
        assertTrue(fixture.check().isEmpty())

        // past the interval → digest fires
        fixture.now += 32 * 60_000L
        val triggered = fixture.check()
        assertEquals(1, triggered.size)
        assertEquals(1, fixture.notifier.posted.size)
        assertTrue(
            fixture.notifier.posted.single().first.contains("EUR/USD"),
            "title names the pair: ${fixture.notifier.posted.single().first}",
        )
    }

    @Test
    fun masterSwitchAndPermissionGateEverything() = runTest(dispatcher) {
        val fixture = Fixture(rates = mapOf("USD" to 2.0))
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.THRESHOLD,
                thresholdPercent = 0.5,
                lastRate = 1.00,
                lastNotifiedAt = 0L,
            )
        )

        fixture.storage.masterSwitch = false
        assertTrue(fixture.check().isEmpty())

        fixture.storage.masterSwitch = true
        fixture.notifier.enabled = false
        assertTrue(fixture.check().isEmpty())
        assertTrue(fixture.notifier.posted.isEmpty())
    }

    @Test
    fun disabledAlertsAreIgnored() = runTest(dispatcher) {
        val fixture = Fixture(rates = mapOf("USD" to 2.0))
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.THRESHOLD,
                thresholdPercent = 0.5,
                lastRate = 1.00,
                enabled = false,
            )
        )

        assertTrue(fixture.check().isEmpty())
        assertTrue(fixture.notifier.posted.isEmpty())
    }

    @Test
    fun offlineCheckKeepsPreviousState() = runTest(dispatcher) {
        val fixture = Fixture(rates = null)
        fixture.repository.seed(
            RateAlert(
                base = "EUR",
                quote = "USD",
                mode = RateAlertMode.PERIODIC,
                intervalMinutes = RateAlert.INTERVAL_HOURLY,
                lastRate = 1.05,
                lastNotifiedAt = 0L,
            )
        )

        assertTrue(fixture.check().isEmpty())
        assertEquals(1.05, fixture.repository.alerts.single().lastRate)
    }

    private companion object {
        // never used at runtime — the engine takes the scripted fetchRates
        private val dummyApi = CurrencyApi(
            httpClient = io.ktor.client.HttpClient(),
            accessKey = "",
        )
    }
}
