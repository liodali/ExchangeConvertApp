package dali.hamza.shared.data.repository

import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.domain.models.Currency
import dali.hamza.shared.domain.models.ExchangeRate
import dali.hamza.shared.domain.models.MyResponse
import dali.hamza.shared.domain.repository.IRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CurrencyRepositoryImpl(
    private val currencyApi: CurrencyApi,
) : IRepository {
    
    override suspend fun getListCurrencies(): MyResponse<List<Currency>> {
        return currencyApi.getCurrencies()
            .map { apiModels ->
                apiModels.map { apiModel ->
                    Currency(
                        name = apiModel.code,
                        fullCountryName = apiModel.description
                    )
                }
            }
            .fold(
                onSuccess = { MyResponse.Success(it) },
                onFailure = { MyResponse.Error(it) }
            )
    }
    
    override suspend fun saveListCurrencies(): Flow<MyResponse<List<Currency>>> {
        return flow {
            emit(getListCurrencies())
        }
    }
    
    override suspend fun saveExchangeRatesOfCurrentCurrency() {
        // Implementation will use platform-specific storage
        // This will be implemented with expect/actual
    }
    
    override suspend fun getListRatesCurrencies(amount: Double): MyResponse<List<ExchangeRate>> {
        // Implementation will fetch rates and calculate amounts
        // This will be implemented with platform-specific storage
        return MyResponse.Error("Not yet implemented - database access pending")
    }
    
    override suspend fun getCurrentCurrency(): String {
        // Implementation will use platform-specific storage
        throw NotImplementedError("Implementation pending - platform-specific storage required")
    }
}
