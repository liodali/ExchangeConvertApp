@file:OptIn(ExperimentalCoroutinesApi::class)

package dali.hamza.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dali.hamza.core.common.SessionManager
import dali.hamza.domain.common.DateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private val testContext: Context = ApplicationProvider.getApplicationContext()
    private val testCoroutineDispatcher =
        StandardTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())
    private val testDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = testCoroutineScope,
            produceFile =
            { testContext.preferencesDataStoreFile("test_pref") }
        )
    private val sessionManagerTest: SessionManager = SessionManager(testDataStore)

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    @Throws(IOException::class)
    fun close() {
        Dispatchers.resetMain()

        testCoroutineScope.cancel()
        //testCoroutineScope.cancel()
    }

    @Test
    fun testEmptyLastUTime() =
        testCoroutineScope.runTest {
            val uDate = sessionManagerTest.getLastUTimeUpdateRates.first()
            assertEquals(Date(0L), uDate)
        }

    @Test
    fun testLastUTime() =
        testCoroutineScope.runTest {
            val now = DateManager.now().time
            async {
                sessionManagerTest.setTimeLastUpdateRate(now)
            }.await()
            val uDate = sessionManagerTest.getLastUTimeUpdateRates.first()
            assert(now == uDate.time)
        }

    @Test
    fun testClean() =
        testCoroutineScope.runTest {

            async {
                sessionManagerTest.clear()
            }.await()
            val uDate = sessionManagerTest.getLastUTimeUpdateRates.first()
            assert(Date(0L) == uDate)
        }


}