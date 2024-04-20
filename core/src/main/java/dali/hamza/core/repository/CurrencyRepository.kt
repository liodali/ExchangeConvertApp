package dali.hamza.core.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dali.hamza.core.common.SessionManager
import dali.hamza.core.common.data
import dali.hamza.core.common.simpleData
import dali.hamza.core.common.toCurrencyEntity
import dali.hamza.core.common.toHistoricRatesEntity
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
import kotlinx.coroutines.withContext
import mohamedali.hamza.database.commons.toRateEntity
import mohamedali.hamza.database.dao.CurrencyDao
import mohamedali.hamza.database.dao.HistoricRateDao
import mohamedali.hamza.database.dao.RatesCurrencyDao
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


    override suspend fun getListCurrencies(): IResponse {
        val list = currencyDao.getListCurrencies()
        return when (list.isNotEmpty()) {
            true -> MyResponse.SuccessResponse(list)

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
                    return MyResponse.ErrorResponse<Any>(EmptyResponse)
                }
                val mappedCurrencies = currencies.data!!.map { currencyJson ->
                    currencyJson.toCurrencyEntity()
                }
                currencyDao.insertAll(mappedCurrencies)
                return MyResponse.SuccessResponse(currencies.data!!)
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
                        when {

                            diff.hours >= 1 && list.isEmpty() -> async {
                                val rates = getRatesFromApi(
                                    currency = currentCurrency,
                                )
                                saveRatesLocally(rates, currentCurrency)
                            }.await()

                            diff.hours >= 1 -> async {
                                archivedRatesByCurrency(currentCurrency)
                                val rates = getRatesFromApi(
                                    currency = currentCurrency,
                                )
                                saveRatesLocally(rates, currentCurrency)
                            }.await()

                        }
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
        val currencies = currencyClientAPI.getRatesListCurrencies(
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
        sessionManager.setTimeNowLastUpdateRate()
        return currencies
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