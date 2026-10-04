package dali.hamza.shared.platform

/**
 * True on iOS, false elsewhere. Used for small platform-specific sizing
 * tweaks in the shared UI (e.g. the bottom nav — iOS safe-area insets
 * differ from Android's).
 */
expect fun isIos(): Boolean
