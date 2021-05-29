package dali.hamza.core

import com.squareup.moshi.Moshi
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.RateConverter
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RateRequestUnitTest {
    private var mockWebServer = MockWebServer()

    val moshi = Moshi.Builder()
        .add(RateConverter())
        .build()

    private lateinit var apiService: CurrencyClientApi


    @Before
    fun setUp() {
        // checkthis blogpost for more details about mock server
        // https://medium.com/@hanru.yeh/unit-test-retrofit-and-mockwebserver-a3e4e81fd2a2
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            // .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(mockWebServer.url("/")) // note the URL is different from production one
            .build()
            .create(CurrencyClientApi::class.java)
        mockWebServer.enqueue(
            MockResponse().setBody(
                "{\n" +
                        "    \"success\": true,\n" +
                        "    \"terms\": \"https://currencylayer.com/terms\",\n" +
                        "    \"privacy\": \"https://currencylayer.com/privacy\",\n" +
                        "    \"timestamp\": 1430401802,\n" +
                        "    \"source\": \"USD\",\n" +
                        "    \"quotes\": {\n" +
                        "        \"USDAED\": 3.672982,\n" +
                        "        \"USDAFN\": 57.8936,\n" +
                        "        \"USDALL\": 126.1652,\n" +
                        "        \"USDAMD\": 475.306,\n" +
                        "        \"USDANG\": 1.78952,\n" +
                        "        \"USDAOA\": 109.216875,\n" +
                        "        \"USDARS\": 8.901966,\n" +
                        "        \"USDAUD\": 1.269072,\n" +
                        "        \"USDAWG\": 1.792375,\n" +
                        "        \"USDAZN\": 1.04945,\n" +
                        "        \"USDBAM\": 1.757305\n" +
                        "    }\n" +
                        "}  "
            )
        );

    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }


    @Test
    fun testParseCUrrenciesJson() = runBlocking {
        val response = apiService.getRatesListCurrencies(
            "USD"
        )

        print(response.body()?.quotes!!.values.first().rate.values.first() == 3.672982)
    }
}