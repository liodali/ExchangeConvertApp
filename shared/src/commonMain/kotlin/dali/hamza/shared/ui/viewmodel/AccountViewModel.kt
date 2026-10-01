package dali.hamza.shared.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.platform.currentEpochMillis

/**
 * Account cluster state (design frame `qDCZE`) — KMP-safe, no platform
 * ViewModel base. Backed ONLY by [ISessionStorage] preference keys
 * (Phase 3 rule: additive, zero data-layer risk).
 */
class AccountViewModel(private val storage: ISessionStorage) {

    /** Profile display name — shared with the dashboard top bar. */
    var username by mutableStateOf(storage.getUsername())
        private set

    /** Push notifications preference (persisted). */
    var notificationsEnabled by mutableStateOf(storage.getNotificationsEnabled())
        private set

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
