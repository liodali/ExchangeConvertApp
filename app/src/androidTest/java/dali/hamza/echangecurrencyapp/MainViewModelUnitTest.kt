package dali.hamza.echangecurrencyapp

import android.content.Context
import androidx.test.runner.AndroidJUnit4
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.db.AppDB
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.CurrencyConverter
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    lateinit var mainViewModel: MainViewModel
    private val context = ApplicationProvider.getApplicationContext<Context>()
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
    lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        mockWebServer.start(8000)
        sessionManager = SessionManager("", context = context)
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
            assert( mainViewModel.mutableFlowAmountForm.amount == "12")
        }
}