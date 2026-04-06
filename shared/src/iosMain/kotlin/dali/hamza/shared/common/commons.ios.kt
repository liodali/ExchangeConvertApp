package dali.hamza.shared.common

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatString(format: String, vararg args: Any?): String {
    return NSString.stringWithFormat(format, *args)
}