package dali.hamza.shared.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.domain.models.DataTier
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.currentEpochMillis
import dali.hamza.shared.ui.theme.LedgerColorPalette
import dali.hamza.shared.ui.theme.LedgerThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Account cluster state (design frame `qDCZE`) — KMP-safe, no platform
 * ViewModel base. Backed by [ISessionStorage] preference keys; the
 * repository is optional (guest-mode ledger clearing).
 */
class AccountViewModel(
    private val storage: ISessionStorage,
    private val repository: IRepository? = null,
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Profile display name — shared with the dashboard top bar. */
    var username by mutableStateOf(storage.getUsername())
        private set

    /** Push notifications preference (persisted). */
    var notificationsEnabled by mutableStateOf(storage.getNotificationsEnabled())
        private set

    /** Account data tier — guests get hourly rates, Sovereign realtime. */
    val dataTier: DataTier get() = storage.getDataTier()

    /** Biometric app lock (persisted, disabled by default). */
    var biometricUnlock by mutableStateOf(storage.getBiometricUnlock())
        private set

    /** Dashboard market-overview codes (persisted; editable in Account). */
    var marketPreferences by mutableStateOf(storage.getMarketPreferences())
        private set

    /** Appearance mode (persisted; Account → Appearance). */
    var themeMode by mutableStateOf(LedgerThemeMode.fromStored(storage.getThemeMode()))
        private set

    /** Dark color palette (persisted; Account → Appearance). */
    var colorPalette by mutableStateOf(LedgerColorPalette.fromStored(storage.getColorPalette()))
        private set

    fun updateBiometricUnlock(enabled: Boolean) {
        biometricUnlock = enabled
        storage.setBiometricUnlock(enabled)
    }

    /** Replace slot [slot] (0–2) of the market preferences with [code]. */
    fun updateMarketPreference(slot: Int, code: String) {
        val next = marketPreferences.toMutableList()
        // dedupe: a code picked for one slot leaves any other slot it was in
        next.removeAll { it == code }
        while (next.size <= slot) next.add(code)
        next[slot] = code
        marketPreferences = next.take(3)
        storage.setMarketPreferences(marketPreferences)
    }

    /** Replace the whole market selection (used when the base changes). */
    fun replaceMarketPreferences(codes: List<String>) {
        marketPreferences = codes.take(3)
        storage.setMarketPreferences(marketPreferences)
    }

    /** Reorder the market preferences (long-press drag in Account/onboarding). */
    fun moveMarketPreference(from: Int, to: Int) {
        if (from == to || from !in marketPreferences.indices || to !in marketPreferences.indices) return
        val next = marketPreferences.toMutableList().apply { add(to, removeAt(from)) }
        marketPreferences = next
        storage.setMarketPreferences(marketPreferences)
    }

    fun updateThemeMode(mode: LedgerThemeMode) {
        themeMode = mode
        storage.setThemeMode(mode.stored)
    }

    fun updateColorPalette(palette: LedgerColorPalette) {
        colorPalette = palette
        storage.setColorPalette(palette.stored)
    }

    /** Design persona email, derived from the username. */
    val email: String
        get() = "${username.lowercase().filter { it.isLetterOrDigit() }}@sovereign.vault"

    fun toggleNotifications() {
        notificationsEnabled = !notificationsEnabled
        storage.setNotificationsEnabled(notificationsEnabled)
    }

    /** "Edit Profile" — persists the new display name. */
    fun rename(value: String) {
        val name = value.trim()
        if (name.isEmpty()) return
        storage.setUsername(name)
        username = storage.getUsername()
    }

    /**
     * Guest-mode "Clear Local Ledger" — wipes every recorded exchange
     * from this device. No-op when the repository isn't wired.
     */
    fun clearLedger(onCleared: () -> Unit = {}) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.clearTransactions()
            onCleared()
        }
    }

    /** Footer copy: "JUST NOW" / "4M AGO" / "2H AGO" (design: "2M AGO"). */
    fun lastSyncLabel(): String {
        val last = storage.getLastUpdate()
        if (last <= 0L) return "NEVER"
        val minutes = ((currentEpochMillis() - last) / 60_000L).toInt()
        return when {
            minutes <= 0 -> "JUST NOW"
            minutes < 60 -> "${minutes}M AGO"
            minutes < 24 * 60 -> "${minutes / 60}H AGO"
            else -> "${minutes / (24 * 60)}D AGO"
        }
    }
}
