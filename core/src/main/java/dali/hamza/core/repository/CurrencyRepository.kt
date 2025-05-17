package dali.hamza.core.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dali.hamza.core.common.ISessionManager
import dali.hamza.core.common.toCurrencyEntity
import dali.hamza.core.common.toHistoricRatesEntity
import dali.hamza.core.datasource.network.models.RatesCurrenciesDataAPI
import dali.hamza.domain.common.DateManager
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.CurrencyRate
import dali.hamza.domain.models.EmptyResponse
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.domain.repository.IRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mohamedali.hamza.database.commons.toRateEntity
import mohamedali.hamza.database.dao.CurrencyDao
import mohamedali.hamza.database.dao.HistoricRateDao
import mohamedali.hamza.database.dao.RatesCurrencyDao
import java.util.Date

class CurrencyRepository(
//    private val currencyClientAPI: CurrencyClientApi,
    private val currencyDao: CurrencyDao,
    private val ratesCurrencyDao: RatesCurrencyDao,
    private val historicRateDao: HistoricRateDao,
    val sessionManager: ISessionManager,
    private val defaultDispatcherContext: CoroutineDispatcher = Dispatchers.Default,
    private val serverURL: String
) : IRepository {

    private val client: HttpClient by lazy {
        HttpClient(CIO.create()) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("HTTP Family Client", " : $message")
                    }
                }
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = serverURL
                }
            }
        }
    }

    override suspend fun getListCurrencies(): IResponse {
        val list = currencyDao.getListCurrencies()
        return when (list.isNotEmpty()) {
            true -> MyResponse.SuccessResponse(list.map { Currency(it.name, it.fullCountryName) })

            false -> {
                val response = client.post("currencies") {
                }
                val currencies = response.body<List<Currency>>()

                if (response.status == HttpStatusCode.OK || currencies.isEmpty()) {
                    return MyResponse.ErrorResponse<Any>(EmptyResponse)
                }

                val mappedCurrencies = currencies.map { currencyJson ->
                    currencyJson.toCurrencyEntity()
                }
                currencyDao.insertAll(mappedCurrencies)
                return MyResponse.SuccessResponse(currencies)
            }
        }

    }

    override suspend fun saveListCurrencies(): Flow<IResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun saveExchangeRatesOfCurrentCurrency(dispatcherContext: CoroutineDispatcher?) {
        withContext(dispatcherContext ?: defaultDispatcherContext) {
            val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
            if (currentCurrency.isNotEmpty()) {
                val lastTimeUpdated = sessionManager.getLastUTimeUpdateRates.first()
                when {
                    lastTimeUpdated != Date(0L) -> {
                        /**
                         * test if time to update list of rate more than 30 min in same currency
                         */
                        val diff = DateManager.difference2Date(lastTimeUpdated)
                        val list = ratesCurrencyDao.getListRatesByCurrencies(currentCurrency)
                        if (diff.hours >= 1) {
                            async {
                                val rates = getRatesFromApi(
                                    currency = currentCurrency,
                                )
                                saveRatesLocally(rates, currentCurrency)
                            }.await()
                        }
                    }

                    else -> {
                        archivedRatesByCurrency(currentCurrency)
                        val rates = getRatesFromApi(
                            currency = currentCurrency,
                        )
                        saveRatesLocally(rates, currentCurrency)
                    }
                }
            }
        }
    }

    private suspend fun saveRatesLocally(listRates: List<CurrencyRate>, currentCurrency: String) {
        sessionManager.setTimeNowLastUpdateRate()
        ratesCurrencyDao.insertAll(listRates.map { rate ->
            rate.toRateEntity(currentCurrency)
        })
    }

    override suspend fun getListRatesCurrencies(amount: Double): IResponse {
        saveExchangeRatesOfCurrentCurrency()
        val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
        val listRates = withContext(IO) {
            async {
                ratesCurrencyDao.getListExchangeRatesCurrencies(
                    amount = amount,
                    currency = currentCurrency
                )
            }.await()


        }
        if (listRates.isNotEmpty()) {
            return MyResponse.SuccessResponse(listRates)
        }

        return MyResponse.ErrorResponse<Any>(EmptyResponse)

    }

    override suspend fun getListRatesCurrenciesPaging(amount: Double): Flow<PagingData<ExchangeRate>> {
        saveExchangeRatesOfCurrentCurrency(defaultDispatcherContext)
        val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
        return Pager(
            PagingConfig(pageSize = 30, enablePlaceholders = true)
        ) {
            ratesCurrencyDao.getPagingListExchangeRatesCurrencies(amount, currentCurrency)
        }.flow


    }


    private suspend fun getRatesFromApi(
        currency: String,
    ): List<CurrencyRate> {
//        val currencies = currencyClientAPI.getRatesListCurrencies(
//            source = currency
//        ).simpleData {
//            it.quotes.map { jsonRate ->
//                CurrencyRate(
//                    name = jsonRate.key,
//                    rate = jsonRate.value,
//                    time = DateManager.now()
//                )
//            }
//        }
        val response = client.get("latest") {
            url {
                parameters.append("base", currency)
            }
        }
        val data = response.body<RatesCurrenciesDataAPI>().rates.map { jsonRate ->
            CurrencyRate(
                name = jsonRate.key,
                rate = jsonRate.value,
                time = DateManager.now()
            )
        }
        sessionManager.setTimeNowLastUpdateRate()
        return data
    }

    private suspend fun archivedRatesByCurrency(currency: String) {

        val historics = ratesCurrencyDao.getListRatesByCurrency(currency)
        if (historics.isNotEmpty()) {
            historicRateDao.insertAll(historics.map {
                it.toHistoricRatesEntity()
            })
            ratesCurrencyDao.deleteAll(historics)
        }

    }


}