package dali.hamza.shared.platform

/** Canonical external links (backend legal pages + store listing). */
object AppLinks {
    const val TERMS_URL = "https://exchange.dev.adetify.com/terms"
    const val PRIVACY_URL = "https://exchange.dev.adetify.com/privacy"
    // Store listing id = applicationId in app/build.gradle.kts (NOT the Android
    // namespace, dali.hamza.echangecurrencyapp — that id has no Play listing).
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.sovereignledger.app"
    // Apple id resolved via `asc apps list --bundle-id dali.hamza.ExchangeConvertApp`.
    const val IOS_APPSTORE_URL = "https://apps.apple.com/app/id6819774405"

    /** App Store "write a review" sheet. */
    const val IOS_REVIEW_URL = "$IOS_APPSTORE_URL?action=write-review"

    /** Review destination for the CURRENT platform — Play on Android, App Store on iOS. */
    val STORE_REVIEW_URL: String
        get() = if (isIos()) IOS_REVIEW_URL else PLAY_URL
}

/** Opens [url] in the platform's external browser / store app. */
expect fun openUri(url: String)

/** The app's versionName as installed on this device (e.g. "0.3.0"). */
expect fun appVersionName(): String
