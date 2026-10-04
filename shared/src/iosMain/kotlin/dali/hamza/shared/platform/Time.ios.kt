package dali.hamza.shared.platform

import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSTimeIntervalSince1970

actual fun currentHourOfDay(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitHour,
        fromDate = NSDate(),
    )
    return components.hour.toInt()
}

actual fun currentEpochMillis(): Long =
    ((NSDate().timeIntervalSinceReferenceDate + NSTimeIntervalSince1970) * 1000.0).toLong()
