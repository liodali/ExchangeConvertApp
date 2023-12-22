package dali.hamza.core.datasource.network

import dali.hamza.core.datasource.network.models.CurrenciesDataAPI
import dali.hamza.core.datasource.network.models.RatesCurrenciesDataAPI
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CurrencyClientApi {

    @GET("list")
    suspend fun getListCurrencies(
        @Query("access_key") accessKey: String
    ): Response<CurrenciesDataAPI>


    @GET("latest")
    suspend  fun getRatesListCurrencies(
        @Query("access_key") accessKey: String,
        @Query("source") source: String
    ): Response<RatesCurrenciesDataAPI>

}