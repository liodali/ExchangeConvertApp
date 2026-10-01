package dali.hamza.shared.data.storage

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
}

const val DEFAULT_CURRENCY = "USD"
const val DEFAULT_USERNAME = "SOVEREIGN"
