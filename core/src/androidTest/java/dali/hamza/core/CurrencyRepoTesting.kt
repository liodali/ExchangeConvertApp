package dali.hamza.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.Preference
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.db.AppDB
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.db.entities.RatesCurrencyEntity
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.common.DateManager
import dali.hamza.domain.models.IResponse
import dali.hamza.domain.repository.IRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import  org.mockito.kotlin.mock
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private interface CurrencyClientApi {}
private open class Api : CurrencyClientApi {}

@RunWith(AndroidJUnit4::class)
class CurrencyRepoTesting {


    private lateinit var rateDao: RatesCurrencyDao
    private lateinit var historicRateDao: HistoricRateDao
    private lateinit var currencyDao: CurrencyDao

    private val mockDataStore = mock<DataStore<Preference>>()
    val Context.dataStore by preferencesDataStore(
        "Testpref"
    )
    private val api: CurrencyClientApi = Api()
    private var mockWebServer = MockWebServer()
    private val usd = "USD"
    private val eur = "EUR"
    private val tnd = "TND"
    private var anotherListRate = arrayListOf(
        RatesCurrencyEntity(
            name = usd,
            rate = 1.0,
            time = DateManager.now(),
            selectedCurrency = tnd
        ),
        RatesCurrencyEntity(
            name = "TND",
            rate = 1.0,
            time = DateManager.now(),
            selectedCurrency = tnd
        ),
        RatesCurrencyEntity(
            name = eur,
            rate = 0.30,
            time = DateManager.now(),
            selectedCurrency = tnd
        ),
        RatesCurrencyEntity(
            name = "AED",
            rate = 1.35,
            time = DateManager.now(),
            selectedCurrency = tnd
        )
    )

    private val moshi = Moshi.Builder()
        .add(RateConverter()).build()

    private lateinit var repository: CurrencyRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db: AppDB = Room.inMemoryDatabaseBuilder(
        context, AppDB::class.java
    ).build()

    @Before
    fun create() {
        mockWebServer.start(8000)
        runBlocking {


            rateDao = db.RatesCurrencyDao()
            historicRateDao = db.HistoricRateDao()
            currencyDao = db.CurrencyDao()

            repository = CurrencyRepository(
                Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    // .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .baseUrl(mockWebServer.url("/")) // note the URL is different from production one
                    .build()
                    .create(dali.hamza.core.datasource.network.CurrencyClientApi::class.java),
                currencyDao,
                rateDao,
                historicRateDao,
                SessionManager(
                    "Testpref", context
                )
            )
            repository.sessionManager.setCurrencySelected(eur)
            repository.sessionManager.removeTimeLastUpdateRate()

            /*
            rateDao.insertAll(
                arrayListOf(
                    RatesCurrencyEntity(
                        name = usd,
                        rate = 1.0,
                        time = DateManager.now(),
                        selectedCurrency = usd
                    ),
                    RatesCurrencyEntity(
                        name = "TND",
                        rate = 2.7,
                        time = DateManager.now(),
                        selectedCurrency = usd
                    ),
                    RatesCurrencyEntity(
                        name = eur,
                        rate = 0.82,
                        time = DateManager.now(),
                        selectedCurrency = usd
                    ),
                    RatesCurrencyEntity(
                        name = "AED",
                        rate = 3.76,
                        time = DateManager.now(),
                        selectedCurrency = usd
                    )
                )
            )
            */
        }
    }

    @After
    @Throws(IOException::class)
    fun close() {
        mockWebServer.close()
        db.close()
    }

    @Test
    fun testSimpleAddRatesToDb() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                "{\n" +
                        "    \"success\": true,\n" +
                        "    \"base\": \"EUR\",\n" +
                        "    \"rates\": {\n" +
                        "        \"AED\": 3.672982,\n" +
                        "        \"USD\": 1.18,\n" +
                        "        \"TND\": 3.32\n" +
                        "    }\n" +
                        "}  "
            )
        );
        repository.saveExchangeRatesOfCurrentCurrency()

        val list = rateDao.getListRatesByCurrencies(eur)

        assert(list.isNotEmpty())
    }

    @Test
    fun testSimpleCalculateRates() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                "{\n" +
                        "    \"success\": true,\n" +
                        "    \"base\": \"EUR\",\n" +
                        "    \"rates\": {\n" +
                        "        \"AED\": 3.672982,\n" +
                        "        \"USD\": 1.18,\n" +
                        "        \"TND\": 3.32\n" +
                        "    }\n" +
                        "}  "
            )
        );
        repository.saveExchangeRatesOfCurrentCurrency()

        val list = (rateDao.getListExchangeRatesCurrencies(2.0, eur)).first()

        assert(list[0].calculedAmount == 3.672982 * 2.0)
    }

    @Test
    fun testSimpleAddHistoricRates() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                "{\n" +
                        "    \"success\": true,\n" +
                        "    \"base\": \"EUR\",\n" +
                        "    \"rates\": {\n" +
                        "        \"AED\": 3.672982,\n" +
                        "        \"USD\": 1.18,\n" +
                        "        \"TND\": 3.32\n" +
                        "    }\n" +
                        "}  "
            )
        )
        repository.saveExchangeRatesOfCurrentCurrency()
        repository.sessionManager.setCurrencySelected(usd)
        repository.sessionManager.removeTimeLastUpdateRate()
        mockWebServer.enqueue(
            MockResponse().setBody(
                "{\n" +
                        "    \"success\": true,\n" +
                        "    \"base\": \"USD\",\n" +
                        "    \"rates\": {\n" +
                        "        \"AED\": 3.672982,\n" +
                        "        \"UER\": 0.82,\n" +
                        "        \"TND\": 2.72\n" +
                        "    }\n" +
                        "}  "
            )
        )
        repository.saveExchangeRatesOfCurrentCurrency()

        val list =( historicRateDao.historicRate(eur)).first()

        assert(list.isNotEmpty())
    }

}