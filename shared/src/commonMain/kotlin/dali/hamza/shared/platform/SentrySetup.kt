package dali.hamza.shared.platform

import io.sentry.kotlin.multiplatform.Sentry

/**
 * One Sentry (self-hosted GlitchTip — Sentry protocol) init for BOTH
 * platforms: the KMP SDK drives sentry-android on Android and
 * sentry-cocoa on iOS. Hosts call [start] once per process, before any
 * telemetry use (Android: ExchangeApplication.onCreate; iOS:
 * AppDelegate.didFinishLaunching — background task launches included,
 * the AppDelegate runs there too).
 *
 * Blank DSN = disabled: local dev and CI template builds stay fully
 * functional with crash reporting off.
 */
object SentrySetup {
    fun start(dsn: String, environment: String, release: String, debug: Boolean = false) {
        if (dsn.isBlank()) return
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
            options.release = release
            options.debug = debug
        }
    }
}
