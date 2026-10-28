package dali.hamza.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUri(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>()) { _ -> }
}

actual fun appVersionName(): String {
    // mainBundle = the app — bundleForClass(UIDevice) would return the
    // system UIKit bundle and report ITS version (shows "1.0" in the UI).
    val version = NSBundle.mainBundle.infoDictionary
        ?.get("CFBundleShortVersionString") as? String
    return version ?: "—"
}
