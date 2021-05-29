package dali.hamza.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Named


class SessionManager constructor(
    PREF_NAME: String,
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = PREF_NAME
    )


    companion object {
        val selectedCurrency = stringPreferencesKey("currency")
        val lastTimeFetchForRates = longPreferencesKey("LastTimeRates")
    }

    suspend fun setCurrencySelected(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[selectedCurrency] = currency
        }
    }

    val getCurrencyFromDataStore: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[selectedCurrency] ?: ""
        }

    suspend fun setTimeLastUpdateRate(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[lastTimeFetchForRates] = time
        }
    }

    val getLastUTimeUpdateRates: Flow<Date?> = context.dataStore.data
        .map { preferences ->
            when (preferences.contains(lastTimeFetchForRates)) {
                true -> Date(preferences[lastTimeFetchForRates]!!)
                else -> null
            }
        }

}