package dali.hamza.shared.data.storage

import dali.hamza.shared.domain.models.DataTier

/**
 * Minimal key-value session storage for the shared module
 * (current base currency + last rates update timestamp).
 */
interface ISessionStorage {
    /**
     * Currently selected base currency (ISO code). Defaults to `USD`.
     */
    fun getCurrency(): String

    fun setCurrency(value: String)

    /**
     * Epoch millis of the last successful rates refresh. `0L` = never.
     */
    fun getLastUpdate(): Long

    fun setLastUpdate(timestamp: Long)

    /**
     * Profile display name shown in the top bar (design: the username,
     * e.g. "SOVEREIGN"). Defaults to [DEFAULT_USERNAME] until accounts exist.
     */
    fun getUsername(): String = DEFAULT_USERNAME

    fun setUsername(value: String)

    /**
     * Push notifications preference (Account → Preferences).
     * Defaults to `true`; implementations should persist it.
     */
    fun getNotificationsEnabled(): Boolean = true

    fun setNotificationsEnabled(enabled: Boolean) {}

    /**
     * Preferred pair preset "QUOTE/BASE" (e.g. "USD/EUR") for the dashboard
     * market overview. Defaults to [DEFAULT_PAIR].
     */
    fun getDefaultPair(): String = DEFAULT_PAIR

    fun setDefaultPair(pair: String) {}

    /**
     * Account data tier — guests get hourly-refreshed rates, Sovereign
     * (logged-in) gets realtime. Defaults to guest; the future login flow
     * flips it.
     */
    fun getDataTier(): DataTier = DataTier.GUEST

    fun setDataTier(tier: DataTier) {}
}

const val DEFAULT_CURRENCY = "USD"
const val DEFAULT_USERNAME = "SOVEREIGN"
const val DEFAULT_PAIR = "EUR/USD"
