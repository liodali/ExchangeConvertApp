package dali.hamza.core.repository

import dali.hamza.core.common.SessionManager
import dali.hamza.core.common.data
import dali.hamza.core.common.simpleData
import dali.hamza.core.common.toCurrencyEntity
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.domain.common.DateManager
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.CurrencyRate
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.domain.repository.IRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class CurrencyRepository @Inject constructor(
    private val currencyClientAPI: CurrencyClientApi,
    @Named("token") private val TOKEN_APP: String,
    private val currencyDao: CurrencyDao,
    private val ratesCurrencyDao: RatesCurrencyDao,
    private val historicRateDao: HistoricRateDao,
) : IRepository {

    @Inject
    lateinit var sessionManager: SessionManager


    override suspend fun getListCurrencies(): Flow<IResponse> {
        return flow {
            currencyDao.getListCurrencies().collect { list ->
                when (list.isNotEmpty()) {
                    true -> {
                        emit(MyResponse.SuccessResponse(list))
                    }
                    false -> {
                        val currencies = currencyClientAPI
                            .getListCurrencies(TOKEN_APP).data {
                                it.currencies.values.map { m ->
                                    val list = m.currency.map { c ->
                                        Currency(
                                            name = c.key,
                                            fullCountryName = c.value
                                        )
                                    }
                                    list
                                }.first()
                            }
                        val mappedCurrencies = currencies.data!!.map { c ->
                            c.toCurrencyEntity()
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

    override suspend fun saveExchangeRatesOfCurrentCurrency() {
        withContext(IO) {
            val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
            if (currentCurrency.isNotEmpty()) {
                val lastTimeUpdated = sessionManager.getLastUTimeUpdateRates.first()

                val listRates = when (lastTimeUpdated != null) {
                    true -> {
                        val diff = DateManager.difference2Date(lastTimeUpdated)
                        if (diff.days > 0 || diff.hours > 0) {
                            getRatesFromApi(
                                currency = currentCurrency,
                                token = TOKEN_APP
                            )
                        }
                        null
                    }
                    else -> {
                        getRatesFromApi(
                            currency = currentCurrency,
                            token = TOKEN_APP
                        )
                    }
                }
                if (listRates != null && listRates.isNotEmpty()) {
                    ratesCurrencyDao.insertAll(listRates.map { r ->
                        RatesCurrencyEntity(
                            name = r.name,
                            rate = r.rate,
                            time = r.time,
                            selectedCurrency = currentCurrency
                        )
                    })
                }
            }
        }
    }

    override suspend fun getListRatesCurrencies(amount: Double): Flow<IResponse> {
        TODO("Not yet implemented")
    }


    suspend fun getRatesFromApi(
        currency: String,
        token: String
    ): List<CurrencyRate> {
        val response = currencyClientAPI.getRatesListCurrencies(
            token, source = currency
        ).simpleData {
            it.quotes.values.map { rates ->
                rates.rate.map { r ->
                    CurrencyRate(
                        name = r.key,
                        rate = r.value,
                        time = DateManager.now()
                    )
                }
            }.first()
        }

        return response
    }

}