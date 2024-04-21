package dali.hamza.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dali.hamza.domain.common.DateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

interface ISessionManager {
    val getLastUTimeUpdateRates: Flow<Date>
    val getCurrencyFromDataStore: Flow<String>
    suspend fun setCurrencySelected(currency: String)

    suspend fun setTimeNowLastUpdateRate()
    suspend fun setTimeLastUpdateRate(time: Long)

    suspend fun removeTimeLastUpdateRate()
    suspend fun clear()
}
class SessionManager(
    private val dataStore: DataStore<Preferences>
) :ISessionManager{

    internal constructor(
        prefName: String,
        context: Context
    ) : this(PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(prefName) }
    ))
    /* private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
         name = PREF_NAME
     )*/


    companion object {
        val selectedCurrency = stringPreferencesKey("currency")
        val lastTimeFetchForRates = longPreferencesKey("LastTimeRates")
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun setCurrencySelected(currency: String) {
        dataStore.edit { preferences ->
            preferences.putAll(selectedCurrency to currency)
        }
    }

    override  val getCurrencyFromDataStore: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[selectedCurrency] ?: ""
        }

    override suspend fun setTimeLastUpdateRate(time: Long) {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = time
        }
    }

    override suspend fun setTimeNowLastUpdateRate() {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = DateManager.now().time
        }
    }


    override  suspend fun removeTimeLastUpdateRate() {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = 0L
        }
    }

    override val getLastUTimeUpdateRates: Flow<Date> = dataStore.data
        .map { preferences ->
            when (preferences.contains(lastTimeFetchForRates)) {
                true -> Date(preferences[lastTimeFetchForRates]!!)
                else -> Date(0L)
            }
        }

}