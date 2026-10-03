package dali.hamza.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

actual fun openUri(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>()) { _ -> }
}

actual fun appVersionName(): String {
    val version = NSBundle.bundleForClass(UIDevice).infoDictionary
        ?.get("CFBundleShortVersionString") as? String
    return version ?: "—"
}
