package dali.hamza.shared.platform

/**
 * Aligns system-bar (status bar) icon contrast with the app theme.
 * Hosts with no system bars of their own implement this as a no-op.
 */
expect fun applySystemBarIcons(darkTheme: Boolean)
