package dali.hamza.core.repository

import androidx.paging.PagingConfig
import dali.hamza.core.common.*
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.db.entities.HistoricRatesCurrencyEntity
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.domain.common.DateManager
import dali.hamza.domain.repository.IRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import androidx.paging.Pager
import androidx.paging.PagingData
import dali.hamza.domain.models.*
import dali.hamza.domain.models.Currency
import kotlinx.coroutines.async

class CurrencyRepository @Inject constructor(
    private val currencyClientAPI: CurrencyClientApi,
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
                            .getListCurrencies().data {
                                it.symbols.values.map { m ->
                                    val list = m.map { c ->
                                        Currency(
                                            name = c.value.code,
                                            fullCountryName = c.value.description
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
        var ratesFromLocal = false
        withContext(IO) {
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
                            else -> emptyList()
                        }
                    }
                    else -> {
                        /**
                         * get rates because currency has been changed
                         */
                        val date = ratesCurrencyDao.getLastTimeUpdated(currentCurrency)
                        when (date != null) {
                            true -> {

                                val diff = DateManager.difference2Date(date)
                                when (diff.days < 0 || diff.hours == 0 || diff.minutes < 30) {
                                    true -> {
                                        ratesFromLocal = true
                                        emptyList()
                                    }
                                    else ->
                                        ratesCurrencyDao.getListRatesByCurrencies(
                                            selectedCurreny = currentCurrency
                                        ).map {
                                            CurrencyRate(
                                                name = it.name,
                                                rate = it.rate,
                                                time = it.time
                                            )
                                        }
                                }
                            }
                            false -> {
                                sessionManager.setTimeLastUpdateRate(DateManager.now().time)
                                getRatesFromApi(
                                    currency = currentCurrency,
                                )
                            }
                        }
                    }
                }

                if (listRates.isNotEmpty() && !ratesFromLocal) {
                    sessionManager.setTimeNowLastUpdateRate()
                    archivedRatesByCurrency(currentCurrency)
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

        return flow {
            saveExchangeRatesOfCurrentCurrency()
            val currentCurrency = sessionManager.getCurrencyFromDataStore.first()
            ratesCurrencyDao.getListExchangeRatesCurrencies(
                amount = amount,
                currency = currentCurrency
            ).collect { list ->
                if (list.isNotEmpty()) {
                    emit(MyResponse.SuccessResponse(list))
                }
            }
        }


    }

    override suspend fun getListRatesCurrenciesPaging(amount: Double): Flow<PagingData<ExchangeRate>> {
        saveExchangeRatesOfCurrentCurrency()
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
            source = currency
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
    }

    private suspend fun archivedRatesByCurrency(
        currency: String
    ) {

        val historics = ratesCurrencyDao.getListRatesByCurrencies(currency)
        if (historics.isNotEmpty()) {
            historicRateDao.insertAll(historics.map {
                it.toHistoricRatesEntity()
            })
            ratesCurrencyDao.deleteAll(historics)
        }

    }


}