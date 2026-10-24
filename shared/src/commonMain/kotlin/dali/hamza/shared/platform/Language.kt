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

/**
 * Applies an in-app language override picked from the language sheet.
 *
 * - null → follow the phone language again
 * - tag (BCP-47, one of [dali.hamza.shared.data.storage.SUPPORTED_LANGUAGE_TAGS])
 *   → the app UI switches to it
 *
 * Android recreates the activity immediately (AppCompatDelegate routes to
 * the system LocaleManager on 13+, backports below). iOS needs one relaunch
 * — the actual presents the restart confirmation itself.
 */
expect fun applyLanguageOverride(tag: String?)
