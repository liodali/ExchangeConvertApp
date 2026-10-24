package dali.hamza.shared.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun currentLanguageTag(): String {
    // preferredLanguages entries are already BCP-47 ("de-US", "pt-PT");
    // the region is kept — CLDR currency names differ (pt vs pt-PT).
    // Fall back to the current locale's language code.
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? NSString
    return preferred?.toString()?.takeIf { it.isNotBlank() }
        ?: NSLocale.currentLocale.languageCode
}

actual fun openLanguageSettings() {
    // The app's Settings pane carries "Preferred Language" once the app
    // ships localizations (Localizable.xcstrings).
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { _ -> }
}
