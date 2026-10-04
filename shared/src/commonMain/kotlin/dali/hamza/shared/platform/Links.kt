package dali.hamza.shared.platform

/** Canonical external links (backend legal pages + store listing). */
object AppLinks {
    const val TERMS_URL = "https://exchange.dev.adetify.com/terms"
    const val PRIVACY_URL = "https://exchange.dev.adetify.com/privacy"
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.sovereignledger.app"
}

/** Opens [url] in the platform's external browser / store app. */
expect fun openUri(url: String)

/** The app's versionName as installed on this device (e.g. "0.3.0"). */
expect fun appVersionName(): String
