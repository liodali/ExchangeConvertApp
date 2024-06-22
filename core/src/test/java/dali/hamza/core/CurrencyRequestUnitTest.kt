package dali.hamza.core

import com.google.gson.Gson
import com.squareup.moshi.Moshi
import dali.hamza.core.datasource.network.CurrencyClientApi
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
        //.add(CurrencyConverter())
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

        val innerJ = arrayOf(
            mapOf(
                "currency" to "AED",
                "name" to "United Arab Emirates Dirham",
            ),

            mapOf(
                "currency" to "AFN",
                "name" to "Afghan Afghan",
            ),
            mapOf(
                "currency" to "ALL",
                "name" to "Albanian Lek",
            ),
        )
        val json = Gson().toJson(
            innerJ
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
        val response = apiService.getListCurrencies()
        assert(response.body()!!.first()["currency"] == "AED")
    }
}