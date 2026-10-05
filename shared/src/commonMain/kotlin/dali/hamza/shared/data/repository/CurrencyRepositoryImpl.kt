package dali.hamza.shared.data.repository

import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.database.AppDatabase
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.HistoricalRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.models.RateAlert
import dali.hamza.shared.domain.models.RateAlertMode
import dali.hamza.shared.domain.models.Transaction
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.currentEpochMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Minimum delay between two live-rates API calls (logged-in tier). */
private const val REFRESH_INTERVAL_MS = 30L * 60L * 1000L

/** Guest tier: rates refresh at most hourly (guest-mode decision, Oct 2026). */
private const val GUEST_REFRESH_INTERVAL_MS = 60L * 60L * 1000L

class CurrencyRepositoryImpl(
    private val currencyApi: CurrencyApi,
    private val database: AppDatabase,
    private val sessionStorage: ISessionStorage,
) : IRepository {

    override suspend fun getListCurrencies(): MyResponse<List<Currency>> =
        withContext(Dispatchers.Default) {
            val stored = database.currenciesQueries
                .selectCurrencies()
                .executeAsList()
                .map { Currency(name = it.name, fullCountryName = it.fullCountryName) }
            if (stored.isEmpty()) {
                MyResponse.Success(CurrenciesCatalog.currencies)
            } else {
                MyResponse.Success(stored)
            }
        }

    override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> =
        flow {
            CurrenciesCatalog.currencies.forEach { currency ->
                database.currenciesQueries.insertCurrency(
                    name = currency.name,
                    fullCountryName = currency.fullCountryName
                )
            }
            emit(MyResponse.Success(CurrenciesCatalog.currencies))
        }.flowOn(Dispatchers.Default)

    override suspend fun saveExchangeRatesOfCurrentCurrency() = withContext(Dispatchers.Default) {
        val currentCurrency = sessionStorage.getCurrency()
        val now = nowMillis()
        val lastUpdate = sessionStorage.getLastUpdate()
        if (lastUpdate != 0L && now - lastUpdate < refreshIntervalMs(sessionStorage.getDataTier())) {
            return@withContext
        }
        currencyApi.getLiveRates(currentCurrency)
            .fold(
                onSuccess = { data ->
                    // exchange-api shape: rates keyed by quote code {"EUR": 0.88}
                    val rates = data.rates
                    if (rates != null) {
                        // server "time" is a display string — store fetch time
                        database.currenciesQueries.deleteAllRates()
                        rates.forEach { (target, rate) ->
                            if (target != currentCurrency) {
                                database.currenciesQueries.insertRate(
                                    name = target,
                                    rate = rate,
                                    time = now,
                                    baseCurrency = currentCurrency
                                )
                            }
                        }
                        sessionStorage.setLastUpdate(now)
                    }
                },
                onFailure = { /* keep previously stored rates when the API is unreachable */ }
            )
        Unit
    }

    override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> =
        withContext(Dispatchers.Default) {
            saveExchangeRatesOfCurrentCurrency()
            val base = sessionStorage.getCurrency()
            val stored = database.currenciesQueries
                .selectRatesByBaseCurrency(base)
                .executeAsList()
            if (stored.isEmpty()) {
                return@withContext MyResponse.Error("Rates unavailable for $base")
            }
            MyResponse.Success(
                stored.map { row ->
                    ExchangeRate(
                        name = row.name,
                        rate = row.rate,
                        calculatedAmount = amount * row.rate,
                        time = row.time
                    )
                }
            )
        }

    override suspend fun getCurrentCurrency(): String =
        withContext(Dispatchers.Default) { sessionStorage.getCurrency() }

    override suspend fun getUsername(): String =
        withContext(Dispatchers.Default) { sessionStorage.getUsername() }

    override suspend fun setCurrentCurrency(value: String) = withContext(Dispatchers.Default) {
        if (sessionStorage.getCurrency() != value) {
            sessionStorage.setCurrency(value)
            sessionStorage.setLastUpdate(0L)
        }
        Unit
    }

    override suspend fun refreshExchangeRates() = withContext(Dispatchers.Default) {
        // Guest tier: even manual refreshes respect the hourly window —
        // guests see hourly data, login unlocks realtime (DataTier.SOVEREIGN).
        val now = nowMillis()
        val lastUpdate = sessionStorage.getLastUpdate()
        if (lastUpdate != 0L && now - lastUpdate < refreshIntervalMs(sessionStorage.getDataTier())) {
            return@withContext
        }
        sessionStorage.setLastUpdate(0L)
        saveExchangeRatesOfCurrentCurrency()
    }

    override suspend fun getHistoricalRates(
        base: String,
        symbol: String,
        from: String,
        to: String,
    ): MyResponse<List<HistoricalRate>> = withContext(Dispatchers.Default) {
        currencyApi.getHistoricalSeries(base, symbol, from, to)
            .fold(
                onSuccess = { dto ->
                    val series = dto.rates
                        ?.mapNotNull { (date, symbols) ->
                            val rate = symbols[symbol]
                            if (rate != null) HistoricalRate(date, rate) else null
                        }
                        ?.sortedBy { it.date }
                        .orEmpty()
                    if (series.isEmpty()) {
                        MyResponse.Error("No historical rates for $base/$symbol")
                    } else {
                        MyResponse.Success(series)
                    }
                },
                onFailure = { MyResponse.Error(it.message ?: "Historical rates unavailable") },
            )
    }

    override suspend fun getHistoricalRates(
        base: String,
        symbols: List<String>,
        from: String,
        to: String,
    ): MyResponse<Map<String, List<HistoricalRate>>> = withContext(Dispatchers.Default) {
        currencyApi.getHistoricalSeries(base, symbols.joinToString(","), from, to)
            .fold(
                onSuccess = { dto ->
                    val series = dto.rates.orEmpty()
                        .flatMap { (date, values) ->
                            values.map { (symbol, rate) -> symbol to HistoricalRate(date, rate) }
                        }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, points) -> points.sortedBy { it.date } }
                    if (series.isEmpty()) {
                        MyResponse.Error("No historical rates for $base")
                    } else {
                        MyResponse.Success(series)
                    }
                },
                onFailure = { MyResponse.Error(it.message ?: "Historical rates unavailable") },
            )
    }

    override suspend fun getTransactions(): List<Transaction> =
        withContext(Dispatchers.Default) {
            database.transactionsQueries
                .selectTransactions()
                .executeAsList()
                .map { row ->
                    Transaction(
                        id = row.id,
                        base = row.base,
                        quote = row.quote,
                        amountBase = row.amountBase,
                        amountQuote = row.amountQuote,
                        rate = row.rate,
                        timestamp = row.timestamp,
                        direction = row.direction,
                    )
                }
        }

    override suspend fun recordTransaction(transaction: Transaction) =
        withContext(Dispatchers.Default) {
            database.transactionsQueries.transaction {
                database.transactionsQueries.insertTransaction(
                    base = transaction.base,
                    quote = transaction.quote,
                    amountBase = transaction.amountBase,
                    amountQuote = transaction.amountQuote,
                    rate = transaction.rate,
                    timestamp = transaction.timestamp,
                    direction = transaction.direction,
                )
            }
        }

    override suspend fun clearTransactions(): Unit = withContext(Dispatchers.Default) {
        database.transactionsQueries.deleteAllTransactions()
    }

    // ---- rate alerts (local notifications) -----------------------------

    override suspend fun getRateAlerts(): List<RateAlert> =
        withContext(Dispatchers.Default) {
            database.rateAlertsQueries.selectAlerts().executeAsList().map(::toRateAlert)
        }

    override suspend fun getEnabledRateAlerts(): List<RateAlert> =
        withContext(Dispatchers.Default) {
            database.rateAlertsQueries.selectEnabledAlerts().executeAsList().map(::toRateAlert)
        }

    override suspend fun addRateAlert(alert: RateAlert): Result<RateAlert> =
        withContext(Dispatchers.Default) {
            val existing = database.rateAlertsQueries.selectAlerts().executeAsList()
            if (existing.any { it.base == alert.base && it.quote == alert.quote }) {
                return@withContext Result.failure(IllegalStateException(RateAlert.DUPLICATE_MESSAGE))
            }
            val maxAlerts = RateAlert.maxAlertsForTier(sessionStorage.getDataTier())
            if (existing.size >= maxAlerts) {
                return@withContext Result.failure(IllegalStateException(RateAlert.LIMIT_MESSAGE))
            }
            val now = currentEpochMillis()
            database.rateAlertsQueries.insertAlert(
                base = alert.base,
                quote = alert.quote,
                mode = alert.mode.name,
                intervalMinutes = alert.intervalMinutes,
                thresholdPercent = alert.thresholdPercent,
                enabled = 1L,
                lastRate = null,
                // seeded with the creation time so a periodic alert waits a
                // full interval before its first notification
                lastNotifiedAt = now,
                createdAt = now,
            )
            val id = database.rateAlertsQueries.lastInsertRowId().executeAsOne()
            Result.success(
                alert.copy(id = id, enabled = true, lastNotifiedAt = now, createdAt = now)
            )
        }

    override suspend fun removeRateAlert(id: Long): Unit = withContext(Dispatchers.Default) {
        database.rateAlertsQueries.deleteAlert(id)
    }

    override suspend fun setRateAlertEnabled(id: Long, enabled: Boolean): Unit =
        withContext(Dispatchers.Default) {
            // boolean stored as 0/1 INTEGER (sqlite-3.25 dialect)
            database.rateAlertsQueries.updateAlertEnabled(if (enabled) 1L else 0L, id)
        }

    override suspend fun updateRateAlertState(
        id: Long,
        lastRate: Double?,
        lastNotifiedAt: Long,
    ): Unit = withContext(Dispatchers.Default) {
        database.rateAlertsQueries.updateAlertState(lastRate, lastNotifiedAt, id)
    }

    private fun toRateAlert(
        row: dali.hamza.shared.database.RateAlert,
    ): RateAlert = RateAlert(
        id = row.id,
        base = row.base,
        quote = row.quote,
        mode = runCatching { RateAlertMode.valueOf(row.mode) }
            .getOrDefault(RateAlertMode.PERIODIC),
        intervalMinutes = row.intervalMinutes,
        thresholdPercent = row.thresholdPercent,
        enabled = row.enabled != 0L,
        lastRate = row.lastRate,
        lastNotifiedAt = row.lastNotifiedAt,
        createdAt = row.createdAt,
    )

    companion object {
        /**
         * Live-rates refresh window per data tier — guests hourly, Sovereign
         * (login, coming soon) every 30 minutes.
         */
        fun refreshIntervalMs(tier: DataTier): Long = when (tier) {
            DataTier.GUEST -> GUEST_REFRESH_INTERVAL_MS
            DataTier.SOVEREIGN -> REFRESH_INTERVAL_MS
        }
    }
}
