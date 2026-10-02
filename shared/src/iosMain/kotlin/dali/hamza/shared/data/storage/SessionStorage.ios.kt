package dali.hamza.shared.data.storage

import dali.hamza.shared.domain.models.DataTier
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation backed by NSUserDefaults.
 */
class IosSessionStorage : ISessionStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getCurrency(): String =
        defaults.stringForKey(KEY_CURRENCY) ?: DEFAULT_CURRENCY

    override fun setCurrency(value: String) {
        defaults.setObject(value, forKey = KEY_CURRENCY)
    }

    override fun getLastUpdate(): Long =
        defaults.doubleForKey(KEY_LAST_UPDATE).toLong()

    override fun getUsername(): String =
        defaults.stringForKey(KEY_USERNAME) ?: DEFAULT_USERNAME

    override fun setUsername(value: String) {
        defaults.setObject(value, forKey = KEY_USERNAME)
    }

    override fun setLastUpdate(timestamp: Long) {
        defaults.setObject(timestamp.toDouble(), forKey = KEY_LAST_UPDATE)
    }

    override fun getNotificationsEnabled(): Boolean =
        // boolForKey returns NO for absent keys — check presence so the
        // default stays `true` until the user opts out.
        if (defaults.objectForKey(KEY_NOTIFICATIONS) == null) {
            true
        } else {
            defaults.boolForKey(KEY_NOTIFICATIONS)
        }

    override fun setNotificationsEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_NOTIFICATIONS)
    }

    override fun getDefaultPair(): String =
        defaults.stringForKey(KEY_DEFAULT_PAIR) ?: DEFAULT_PAIR

    override fun setDefaultPair(pair: String) {
        defaults.setObject(pair, forKey = KEY_DEFAULT_PAIR)
    }

    override fun getDataTier(): DataTier =
        when (defaults.stringForKey(KEY_DATA_TIER)) {
            DataTier.SOVEREIGN.name -> DataTier.SOVEREIGN
            else -> DataTier.GUEST
        }

    override fun setDataTier(tier: DataTier) {
        defaults.setObject(tier.name, forKey = KEY_DATA_TIER)
    }

    override fun getBiometricUnlock(): Boolean =
        if (defaults.objectForKey(KEY_BIOMETRIC_UNLOCK) == null) {
            false
        } else {
            defaults.boolForKey(KEY_BIOMETRIC_UNLOCK)
        }

    override fun setBiometricUnlock(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_BIOMETRIC_UNLOCK)
    }

    private companion object {
        const val KEY_DATA_TIER = "data_tier"
        const val KEY_BIOMETRIC_UNLOCK = "biometric_unlock"
        const val KEY_CURRENCY = "currency"
        const val KEY_USERNAME = "username"
        const val KEY_LAST_UPDATE = "last_time_update_rates"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DEFAULT_PAIR = "default_pair"
    }
}

actual fun createSessionStorage(): ISessionStorage = IosSessionStorage()
