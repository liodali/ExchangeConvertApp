package dali.hamza.shared.platform

/**
 * iOS: the SwiftUI host owns the status-bar style; the shared framework
 * has no direct window access — no-op (hosts follow the theme via
 * preferredStatusBarStyle if wired).
 */
actual fun applySystemBarIcons(darkTheme: Boolean) = Unit
