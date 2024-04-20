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


class SessionManager(
    private val dataStore: DataStore<Preferences>
) {

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

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun setCurrencySelected(currency: String) {
        dataStore.edit { preferences ->
            preferences.putAll(selectedCurrency to currency)
        }
    }

    val getCurrencyFromDataStore: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[selectedCurrency] ?: ""
        }

    suspend fun setTimeLastUpdateRate(time: Long) {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = time
        }
    }

    suspend fun setTimeNowLastUpdateRate() {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = DateManager.now().time
        }
    }


    suspend fun removeTimeLastUpdateRate() {
        dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = 0L
        }
    }

    val getLastUTimeUpdateRates: Flow<Date> = dataStore.data
        .map { preferences ->
            when (preferences.contains(lastTimeFetchForRates)) {
                true -> Date(preferences[lastTimeFetchForRates]!!)
                else -> Date(0L)
            }
        }

}