package dali.hamza.shared.domain.models

/**
 * Account data tier (guest-mode decision, Oct 2026):
 * - [GUEST]: no login — rates refresh at most hourly (1h-stale data)
 * - [SOVEREIGN]: logged in — realtime rates (login ships later)
 */
enum class DataTier(val label: String) {
    GUEST("Guest"),
    SOVEREIGN("Sovereign"),
}
