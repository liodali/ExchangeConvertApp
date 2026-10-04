package dali.hamza.shared.common

import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.stringWithFormat

actual fun formatString(format: String, value: Double): String {
    return NSString.stringWithFormat(format, value)
}

actual fun nowMillis(): Long =
    (NSDate.date().timeIntervalSince1970 * 1000.0).toLong()
