package dali.hamza.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.posix.exit

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

/**
 * Writes the per-app language the same way iOS Settings does
 * (`AppleLanguages` in the standard defaults) — it takes effect on the next
 * launch, so the user gets a native restart confirmation right here.
 */
actual fun applyLanguageOverride(tag: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag.isNullOrBlank()) {
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    } else {
        defaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES_KEY)
    }
    defaults.synchronize()
    showRestartConfirmation()
}

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

private fun localized(key: String): String =
    NSBundle.mainBundle.localizedStringForKey(key, null, null) ?: key

private fun showRestartConfirmation() {
    val root: UIViewController? =
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)
                ?.rootViewController
    // nothing to present on (very early launch) — the stored override still
    // applies on the next launch
    if (root == null || root.presentedViewController != null) return
    val alert = UIAlertController.alertControllerWithTitle(
        title = null,
        message = localized("lang_restart_note"),
        preferredStyle = UIAlertControllerStyleAlert,
    )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            localized("lang_restart_now"),
            UIAlertActionStyleDefault,
        ) { exit(0) },
    )
    alert.addAction(
        UIAlertAction.actionWithTitle(
            localized("lang_restart_later"),
            UIAlertActionStyleCancel,
            null,
        ),
    )
    root.presentViewController(alert, animated = true, completion = null)
}
