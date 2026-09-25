package dali.hamza.shared.data.storage

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

    override fun setLastUpdate(timestamp: Long) {
        defaults.setObject(timestamp.toDouble(), forKey = KEY_LAST_UPDATE)
    }

    private companion object {
        const val KEY_CURRENCY = "currency"
        const val KEY_LAST_UPDATE = "last_time_update_rates"
    }
}

actual fun createSessionStorage(): ISessionStorage = IosSessionStorage()
