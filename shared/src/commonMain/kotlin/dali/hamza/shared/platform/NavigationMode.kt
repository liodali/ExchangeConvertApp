package dali.hamza.shared.platform

/**
 * True when the device uses button navigation (3-button, or the legacy
 * 2-button bar) — i.e. a system navigation bar sits below our edge-to-edge
 * UI. False for full gesture navigation (small gesture inset only).
 */
expect fun usesButtonNavigation(): Boolean
