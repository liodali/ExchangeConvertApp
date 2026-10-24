package dali.hamza.shared.platform

/**
 * The user's current UI language as a BCP-47 tag ("de", "pt-PT"…). Drives
 * the backend's `?lang=` currency-name localization; the UI itself follows
 * the system/app locale via Compose resources.
 */
expect fun currentLanguageTag(): String

/**
 * Opens the platform's per-app language panel (Android 13+: the system
 * per-app locale picker; older Android: the app details page). iOS: the
 * app's Settings pane, which carries "Preferred Language" when localizations
 * are present. The app restarts/recreates itself on change — no in-app state.
 */
expect fun openLanguageSettings()
