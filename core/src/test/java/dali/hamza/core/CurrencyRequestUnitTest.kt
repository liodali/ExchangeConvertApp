package dali.hamza.core

import com.google.gson.Gson
import com.squareup.moshi.Moshi
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.CurrencyConverter
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CurrencyRequestUnitTest {
    private var mockWebServer = MockWebServer()

    val moshi = Moshi.Builder()
        .add(CurrencyConverter())
        .build()

    private lateinit var apiService: CurrencyClientApi


    @Before
    fun setUp() {
        // check this blogpost for more details about mock server
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            // .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(mockWebServer.url("/")) // note the URL is different from production one
            .build()
            .create(CurrencyClientApi::class.java)

        val innerJ = mapOf(
            "AED" to "United Arab Emirates Dirham",
            "AFN" to "Afghan Afghan",
            "ALL" to "Albanian Lek",
        )
        val json = Gson().toJson(
            mapOf(
                "success" to true,
                "terms" to "https://currencylayer.com/terms",
                "privacy" to "https://currencylayer.com/privacy",
                "currencies" to innerJ
            )
        ).toString()
        mockWebServer.enqueue(
            MockResponse().setBody(
                json
            )
        )

    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }


    @Test
    fun testParseCurrenciesJson() = runBlocking {
        val response = apiService.getListCurrencies(
            ""
        )
        assert(response.body()?.currencies!!.keys.first() == "AED")
    }
}