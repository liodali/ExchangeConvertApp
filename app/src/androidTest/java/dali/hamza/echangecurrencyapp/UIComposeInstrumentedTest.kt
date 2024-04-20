package dali.hamza.echangecurrencyapp

import android.content.Context
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.squareup.moshi.Moshi
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.CurrencyConverter
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.echangecurrencyapp.ui.compose.component.ExchangesRatesGrid
import dali.hamza.echangecurrencyapp.ui.compose.theme.ExchangeCurrencyAppTheme
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mohamedali.hamza.database.AppDB
import mohamedali.hamza.database.dao.CurrencyDao
import mohamedali.hamza.database.dao.HistoricRateDao
import mohamedali.hamza.database.dao.RatesCurrencyDao
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class UIInstrumentedTest {

    @get:Rule
    val ruleCompose = createComposeRule()
    private lateinit var mainViewModel: MainViewModel
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val testCoroutineDispatcher =
        StandardTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())
    private val testDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = testCoroutineScope,
            produceFile =
            { context.preferencesDataStoreFile("test_prefs") }
        )


    private val db: AppDB = Room.inMemoryDatabaseBuilder(
        context, AppDB::class.java
    ).build()
    private val moshi = Moshi.Builder()
        .add(RateConverter())
        .add(CurrencyConverter()).build()
    private lateinit var rateDao: RatesCurrencyDao
    private lateinit var historicRateDao: HistoricRateDao
    private lateinit var currencyDao: CurrencyDao


    // private val api: CurrencyClientApi = Api()
    private var mockWebServer = MockWebServer()
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        mockWebServer.start(8000)
        sessionManager = SessionManager(testDataStore)
        rateDao = db.RatesCurrencyDao()
        historicRateDao = db.HistoricRateDao()
        currencyDao = db.CurrencyDao()
        val repository = CurrencyRepository(
            Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                // .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .baseUrl(mockWebServer.url("/")) // note the URL is different from production one
                .build()
                .create(CurrencyClientApi::class.java),
            currencyDao,
            rateDao,
            historicRateDao,
            sessionManager,
            ""
        )
        /*val json = Gson().toJson(
            mapOf(
                "success" to true,
                "terms" to "https://currencylayer.com/terms",
                "privacy" to "https://currencylayer.com/privacy",
                "currencies" to mapOf(
                    "AED" to "United Arab Emirates Dirham",
                    "AFN" to "Afghan Afghan",
                    "ALL" to "Albanian Lek",
                )
            )
        ).toString()*/

        mainViewModel = MainViewModel(repository, sessionManager)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun test_exchangeRate() {
        mainViewModel.setCurrencySelection("EUR")
        mockWebServer.enqueue(
            MockResponse().setBody(
                Gson().toJson(
                    mapOf(
                        "success" to true,
                        "terms" to "https://currencylayer.com/terms",
                        "privacy" to "https://currencylayer.com/privacy",
                        "currencies" to mapOf(
                            "EURAED" to 2.3,
                            "EURAFN" to 5.0,
                            "EURALL" to 2.0,
                        )
                    )
                ).toString()
            )
        )
        mainViewModel.calculateExchangeRates(2.0)
        ruleCompose.setContent {
            ExchangeCurrencyAppTheme {
                ExchangesRatesGrid(viewModel = mainViewModel)
            }
        }
        ruleCompose.onNode(hasTestTag("loading")).assertExists()
        ruleCompose.waitForIdle()
        ruleCompose.waitUntil(timeoutMillis = 7_000) {
            runBlocking {
                delay(5000)
                return@runBlocking true
            }
        }
    }
}