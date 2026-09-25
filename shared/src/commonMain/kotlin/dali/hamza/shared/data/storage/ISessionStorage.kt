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
}

const val DEFAULT_CURRENCY = "USD"
