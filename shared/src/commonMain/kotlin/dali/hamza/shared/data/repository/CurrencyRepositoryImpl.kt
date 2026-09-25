package dali.hamza.shared.data.repository

import dali.hamza.shared.common.nowMillis
import dali.hamza.shared.data.CurrenciesCatalog
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.database.AppDatabase
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Minimum delay between two live-rates API calls. */
private const val REFRESH_INTERVAL_MS = 30L * 60L * 1000L

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
        if (lastUpdate != 0L && now - lastUpdate < REFRESH_INTERVAL_MS) {
            return@withContext
        }
        currencyApi.getLiveRates(currentCurrency)
            .fold(
                onSuccess = { data ->
                    val quotes = data.quotes
                    if (data.success == true && quotes != null) {
                        val time = (data.timestamp ?: (now / 1000)) * 1000
                        database.currenciesQueries.deleteAllRates()
                        quotes.forEach { (key, rate) ->
                            val target = key.removePrefix(currentCurrency)
                            if (target.isNotEmpty() && target != currentCurrency) {
                                database.currenciesQueries.insertRate(
                                    name = target,
                                    rate = rate,
                                    time = time,
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

    override suspend fun setCurrentCurrency(value: String) = withContext(Dispatchers.Default) {
        if (sessionStorage.getCurrency() != value) {
            sessionStorage.setCurrency(value)
            sessionStorage.setLastUpdate(0L)
        }
        Unit
    }

    override suspend fun refreshExchangeRates() = withContext(Dispatchers.Default) {
        sessionStorage.setLastUpdate(0L)
        saveExchangeRatesOfCurrentCurrency()
    }
}
