package dali.hamza.core.datasource.network

import dali.hamza.core.datasource.network.models.RatesCurrenciesDataAPI
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyClientApi {

    @GET("currencies")
    suspend fun getListCurrencies(
    ): Response<List<Map<String, String>>>


    @GET("latest")
    suspend fun getRatesListCurrencies(
        @Query("base") source: String,
        @Query("symbol") symbol: String? = null
    ): Response<RatesCurrenciesDataAPI>

    @GET("historic")
    suspend fun historicCurrency(
        @Query("base") source: String,
        @Query("from") from: String,
        @Query("to") to: String?,
        @Query("symbol") symbol: String?
    ): Response<RatesCurrenciesDataAPI>

}