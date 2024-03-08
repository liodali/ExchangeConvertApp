package dali.hamza.core.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dali.hamza.core.common.SessionManager
import dali.hamza.core.common.data
import dali.hamza.core.common.simpleData
import dali.hamza.core.common.toCurrencyEntity
import dali.hamza.core.common.toHistoricRatesEntity
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.domain.common.DateManager
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.CurrencyRate
import dali.hamza.domain.models.EmptyResponse
import dali.hamza.domain.models.ExchangeRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.domain.repository.IRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Date

class CurrencyRepository(
    private val currencyClientAPI: CurrencyClientApi,
    private val currencyDao: CurrencyDao,
    private val ratesCurrencyDao: RatesCurrencyDao,
    private val historicRateDao: HistoricRateDao,
    val sessionManager: SessionManager,
    private val tokenAPI: String,
    private val defaultDispatcherContext: CoroutineDispatcher = Dispatchers.Default,
) : IRepository {


    override suspend fun getListCurrencies(): Flow<IResponse> {
        return flow {
            currencyDao.getListCurrencies().collect { list ->
                when (list.isNotEmpty()) {
                    true -> {
                        emit(MyResponse.SuccessResponse(list))
                    }

                    false -> {
                        val currencies = currencyClientAPI
                            .getListCurrencies(accessKey = tokenAPI).data { currencies ->
                                currencies.currencies.values.map { mJson ->
                                    mJson.map { currency ->
                                        Currency(
                                            name = currency.key,
                                            fullCountryName = currency.value
                                        )
                                    }

                                }.first()
                            }
                        if (currencies.data == null || currencies.data!!.isEmpty()) {
                            emit(MyResponse.ErrorResponse<Any>(EmptyResponse))
                        }
                        val mappedCurrencies = currencies.data!!.map { currencyJson ->
                            currencyJson.toCurrencyEntity()
                        }
                        currencyDao.insertAll(mappedCurrencies)
                    }
                }
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
                val listRates: List<CurrencyRate> = when (lastTimeUpdated != Date(0L)) {
                    true -> {
                        /**
                         * test if time to update list of rate more than 30 min in same currency
                         */
                        val diff = DateManager.difference2Date(lastTimeUpdated)
                        when (diff.days > 0 || diff.hours > 0 || diff.minutes > 31) {
                            true -> async {
                                getRatesFromApi(
                                    currency = currentCurrency,
                                )
                            }.await()

                            else -> {
                                val list = ratesCurrencyDao.getLastListRatesCurrency()
                                if (list.isNotEmpty()) {
                                    sessionManager.setTimeLastUpdateRate(DateManager.now().time)
                                    getRatesFromApi(
                                        currency = currentCurrency,
                                    )
                                } else {
                                    emptyList()
                                }


                            }
                        }
                    }

                    else -> {
                        /**
                         * get rates because currency has been changed
                         */
                        val list =
                            ratesCurrencyDao.getListRatesByCurrency(currency = currentCurrency)
                        if (list.isNotEmpty()) {
                            archivedRatesByCurrency()
                        }
                        getRatesFromApi(
                            currency = currentCurrency,
                        )
                    }
                }

                if (listRates.isNotEmpty()) {
                    async {
                        saveRatesLocally(listRates, currentCurrency)
                    }.await()
                }
            }
        }
    }

    private suspend fun saveRatesLocally(listRates: List<CurrencyRate>, currentCurrency: String) {
        sessionManager.setTimeNowLastUpdateRate()
        ratesCurrencyDao.insertAll(listRates.map { r ->
            RatesCurrencyEntity(
                name = r.name,
                rate = r.rate,
                time = r.time,
                selectedCurrency = currentCurrency
            )
        })
    }

    override suspend fun getListRatesCurrencies(amount: Double): Flow<IResponse> {
        saveExchangeRatesOfCurrentCurrency(defaultDispatcherContext)
        return flow {

            val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
            /* val date = sessionManager.getLastUTimeUpdateRates.last()

             val diff = DateManager.difference2Date(date)
             if (diff.days > 0 || diff.hours > 0 || diff.minutes > 30) {
                 ratesCurrencyDao.getListExchangeRatesCurrencies(
                     amount = amount,
                     currency = currentCurrency
                 )
             } else {

             }*/
            //getRatesFromApi(currentCurrency)
            val listRates = withContext(IO) {
                async {
                    ratesCurrencyDao.getListExchangeRatesCurrencies(
                        amount = amount,
                        currency = currentCurrency
                    )
                }.await()

            }
            if (listRates.isNotEmpty()) {
                emit(MyResponse.SuccessResponse(listRates))
            } else {
                emit(MyResponse.ErrorResponse<Any>(EmptyResponse))
            }

        }


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
        return currencyClientAPI.getRatesListCurrencies(
            accessKey = tokenAPI,
            source = currency
        ).simpleData {
            it.quotes.values.map { rates ->
                rates.map { r ->
                    CurrencyRate(
                        name = r.key,
                        rate = r.value,
                        time = DateManager.now()
                    )
                }
            }.first()
        }
    }

    private suspend fun archivedRatesByCurrency() {

        val historics = ratesCurrencyDao.getLastListRatesCurrency()
        if (historics.isNotEmpty()) {
            historicRateDao.insertAll(historics.map {
                it.toHistoricRatesEntity()
            })
            ratesCurrencyDao.deleteAll(historics)
        }

    }


}