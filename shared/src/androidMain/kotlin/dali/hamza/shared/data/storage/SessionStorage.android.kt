package dali.hamza.shared.data.storage

import android.content.Context

/**
 * Android implementation backed by SharedPreferences.
 */
class AndroidSessionStorage(context: Context) : ISessionStorage {

    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun getCurrency(): String =
        preferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    override fun setCurrency(value: String) {
        preferences.edit().putString(KEY_CURRENCY, value).apply()
    }

    override fun getLastUpdate(): Long =
        preferences.getLong(KEY_LAST_UPDATE, 0L)

    override fun setLastUpdate(timestamp: Long) {
        preferences.edit().putLong(KEY_LAST_UPDATE, timestamp).apply()
    }

    private companion object {
        const val PREF_NAME = "shared_session"
        const val KEY_CURRENCY = "currency"
        const val KEY_LAST_UPDATE = "last_time_update_rates"
    }
}

actual fun createSessionStorage(): ISessionStorage =
    throw IllegalStateException("Use createSessionStorage(context) for Android")

fun createSessionStorage(context: Context): ISessionStorage = AndroidSessionStorage(context)
