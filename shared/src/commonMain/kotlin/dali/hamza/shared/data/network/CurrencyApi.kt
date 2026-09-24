package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.CurrencyApiModel
import dali.hamza.shared.data.network.models.RatesCurrenciesDataAPI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CurrencyApi(private val httpClient: HttpClient) {

    /**
     * private val baseUrl: String (old attribute)
     * Get list of all supported currencies
     */
    suspend fun getCurrencies(): Result<List<CurrencyApiModel>> {
        return try {
            val response = httpClient.get("currencies")//$baseUrl/
            val list: List<CurrencyApiModel> = response.body()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get latest exchange rates for a base currency
     */
    suspend fun getLatestRates(base: String): Result<RatesCurrenciesDataAPI> {
        return try {
            val response = httpClient.get("latest") {
                parameter("base", base)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get historic exchange rates for a specific date
     */
    suspend fun getHistoricRates(
        base: String,
        date: String,
        symbols: String? = null
    ): Result<RatesCurrenciesDataAPI> {
        return try {
            val response = httpClient.get("historic/$date") {
                parameter("base", base)
                symbols?.let { parameter("symbols", it) }
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
