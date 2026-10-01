package dali.hamza.shared.platform

import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour

actual fun currentHourOfDay(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitHour,
        fromDate = NSDate(),
    )
    return components.hour.toInt()
}
