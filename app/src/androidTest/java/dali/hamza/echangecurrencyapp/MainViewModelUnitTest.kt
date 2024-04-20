package dali.hamza.echangecurrencyapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.CurrencyConverter
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.core.repository.CurrencyRepository
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
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class ViewModelUnitTests {
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
        mainViewModel = MainViewModel(repository, sessionManager)
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        mockWebServer.close()
        db.close()
    }

    @Test
    fun testMainViewModelSetCurrency() =
        runBlocking {
            sessionManager.setCurrencySelected("EUR")
            delay(200)
            assertEquals("EUR", mainViewModel.getCurrencySelection().value)
        }

    @Test
    fun testMainViewModelSetAmount() =
        runBlocking {
            mainViewModel.changeAmount("12")
            delay(200)
            assert(mainViewModel.mutableStateAmountForm.amount == "12")
        }
}