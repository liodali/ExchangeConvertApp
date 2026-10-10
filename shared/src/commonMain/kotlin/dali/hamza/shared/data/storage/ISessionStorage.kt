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

    /**
     * Biometric app lock — disabled by default; when enabled the app asks
     * for device biometrics at launch.
     */
    fun getBiometricUnlock(): Boolean = false

    fun setBiometricUnlock(enabled: Boolean) {}

    /**
     * Market-overview preferences: up to 3 ISO codes quoted against the
     * base currency on the dashboard. An empty list means onboarding has
     * not been completed yet (the market-selection flow runs at first
     * launch; values are editable in Account → Market Preferences).
     */
    fun getMarketPreferences(): List<String> = emptyList()

    fun setMarketPreferences(codes: List<String>) {}

    /**
     * Appearance mode (Account → Appearance): system / light / dark.
     * Defaults to dark — the original design palette.
     */
    fun getThemeMode(): String = "dark"

    fun setThemeMode(mode: String) {}

    /** Dark-mode color palette: sovereign / summer / orchid / forest. */
    fun getColorPalette(): String = "sovereign"

    fun setColorPalette(palette: String) {}

    /**
     * Historical-chart rendering style (Account → Appearance): line / bar.
     * Defaults to line — the original design graph.
     */
    fun getChartStyle(): String = "line"

    fun setChartStyle(style: String) {}

    // ---- server-push alert session (Phase 2, plans/server-push-alerts.md) --
    // Identity for the anonymous session JWT: installId + cached token.

    /**
     * Stable per-install identifier (UUID) minted on first app open —
     * the `sub` of the anonymous session JWT. `null` until first minted.
     */
    fun getInstallId(): String? = null

    fun setInstallId(value: String) {}

    /**
     * Cached anonymous session JWT for the alert endpoints.
     * `null` = never issued (or cleared after a server switch).
     */
    fun getSessionToken(): String? = null

    fun setSessionToken(token: String?) {}

    /** Epoch millis when the session JWT expires. `0L` = unknown. */
    fun getSessionExpiresAt(): Long = 0L

    fun setSessionExpiresAt(timestamp: Long) {}

    /**
     * Last push token this install registered with the server — rotation
     * detection: when a new token registers and differs, the old one is
     * unregistered right after. `null` = never registered.
     */
    fun getLastPushToken(): String? = null

    fun setLastPushToken(token: String?) {}
}

const val DEFAULT_CURRENCY = "USD"
const val DEFAULT_USERNAME = "SOVEREIGN"
const val DEFAULT_PAIR = "EUR/USD"
