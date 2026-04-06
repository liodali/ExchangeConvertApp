package dali.hamza.shared.common

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatString(format: String, value: Double): String {
    return NSString.stringWithFormat(format, value)
}
