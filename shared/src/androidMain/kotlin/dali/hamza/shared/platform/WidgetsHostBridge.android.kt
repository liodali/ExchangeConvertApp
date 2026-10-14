package dali.hamza.shared.platform

/**
 * Android default — the app module registers [WidgetsHostBridgeAndroid]
 * in Koin; this is only the no-Koin fallback (preview/tooling).
 */
actual fun createWidgetsHostBridge(): WidgetsHostBridge = DefaultWidgetsHostBridge
