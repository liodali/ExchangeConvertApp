package dali.hamza.core.repository

import dali.hamza.core.common.SessionManager
import dali.hamza.core.common.data
import dali.hamza.core.common.toCurrencyEntity
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.domain.models.Currency
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.models.MyResponse
import dali.hamza.domain.repository.IRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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

    override suspend fun getExchangeRates(selectedCurrency: String): Flow<IResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getListRatesCurrencies(amount: Double): Flow<IResponse> {
        TODO("Not yet implemented")
    }

}