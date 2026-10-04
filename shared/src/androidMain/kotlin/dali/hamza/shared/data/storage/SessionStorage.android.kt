package dali.hamza.shared.data.storage

import android.content.Context
import dali.hamza.shared.domain.models.DataTier

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

    override fun getUsername(): String =
        preferences.getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME

    override fun setUsername(value: String) {
        preferences.edit().putString(KEY_USERNAME, value).apply()
    }

    override fun setLastUpdate(timestamp: Long) {
        preferences.edit().putLong(KEY_LAST_UPDATE, timestamp).apply()
    }

    override fun getNotificationsEnabled(): Boolean =
        preferences.getBoolean(KEY_NOTIFICATIONS, true)

    override fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    override fun getDefaultPair(): String =
        preferences.getString(KEY_DEFAULT_PAIR, DEFAULT_PAIR) ?: DEFAULT_PAIR

    override fun setDefaultPair(pair: String) {
        preferences.edit().putString(KEY_DEFAULT_PAIR, pair).apply()
    }

    override fun getDataTier(): DataTier =
        when (preferences.getString(KEY_DATA_TIER, null)) {
            DataTier.SOVEREIGN.name -> DataTier.SOVEREIGN
            else -> DataTier.GUEST
        }

    override fun setDataTier(tier: DataTier) {
        preferences.edit().putString(KEY_DATA_TIER, tier.name).apply()
    }

    override fun getBiometricUnlock(): Boolean =
        preferences.getBoolean(KEY_BIOMETRIC_UNLOCK, false)

    override fun setBiometricUnlock(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC_UNLOCK, enabled).apply()
    }

    override fun getMarketPreferences(): List<String> =
        preferences.getString(KEY_MARKET_PREFERENCES, null)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    override fun setMarketPreferences(codes: List<String>) {
        preferences.edit()
            .putString(KEY_MARKET_PREFERENCES, codes.joinToString(","))
            .apply()
    }

    override fun getThemeMode(): String =
        preferences.getString(KEY_THEME_MODE, "dark") ?: "dark"

    override fun setThemeMode(mode: String) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    override fun getColorPalette(): String =
        preferences.getString(KEY_COLOR_PALETTE, "sovereign") ?: "sovereign"

    override fun setColorPalette(palette: String) {
        preferences.edit().putString(KEY_COLOR_PALETTE, palette).apply()
    }

    private companion object {
        const val PREF_NAME = "shared_session"
        const val KEY_CURRENCY = "currency"
        const val KEY_USERNAME = "username"
        const val KEY_LAST_UPDATE = "last_time_update_rates"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DEFAULT_PAIR = "default_pair"
        const val KEY_DATA_TIER = "data_tier"
        const val KEY_BIOMETRIC_UNLOCK = "biometric_unlock"
        const val KEY_MARKET_PREFERENCES = "market_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_COLOR_PALETTE = "color_palette"
    }
}

actual fun createSessionStorage(): ISessionStorage {
    val context = dali.hamza.shared.AndroidAppContext.appContext
        ?: throw IllegalStateException("Set AndroidAppContext.appContext from Application.onCreate() first")
    return createSessionStorage(context)
}

fun createSessionStorage(context: Context): ISessionStorage = AndroidSessionStorage(context)
