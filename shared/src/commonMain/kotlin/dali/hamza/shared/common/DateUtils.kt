package dali.hamza.shared.common

/**
 * Pure-Kotlin UTC date helpers (no datetime dependency): epoch-day ↔
 * ISO `yyyy-MM-dd` via Howard Hinnant's civil-date algorithms.
 *
 * History ranges are whole days; the backend `/historic` speaks ISO dates.
 */
object DateUtils {

    /** Epoch-milliseconds → epoch-day (UTC). */
    fun epochDayOf(millis: Long): Long = floorDiv(millis, 86_400_000L)

    /** Epoch-day → `yyyy-MM-dd`. */
    fun toIsoDate(epochDay: Long): String {
        val (y, m, d) = civilFromDays(epochDay)
        return "${pad(y, 4)}-${pad(m.toLong(), 2)}-${pad(d.toLong(), 2)}"
    }

    /** `yyyy-MM-dd` → epoch-day (returns null for malformed input). */
    fun epochDayOf(isoDate: String): Long? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return daysFromCivil(year.toLong(), month, day)
    }

    /** Format `yyyy-MM-dd HH:mm` for a transaction timestamp (UTC). */
    fun formatDateTime(millis: Long): String {
        val (y, m, d) = civilFromDays(epochDayOf(millis))
        val secondsOfDay = (millis % 86_400_000L) / 1000
        val hour = secondsOfDay / 3600
        val minute = (secondsOfDay % 3600) / 60
        return "${pad(y, 4)}-${pad(m.toLong(), 2)}-${pad(d.toLong(), 2)} " +
            "${pad(hour, 2)}:${pad(minute, 2)}"
    }

    private fun pad(value: Long, width: Int): String =
        value.toString().padStart(width, '0')

    // ---- civil ↔ days (Hinnant, http://howardhinnant.github.io) -----------

    private fun civilFromDays(z0: Long): Triple<Long, Int, Int> {
        val z = z0 + 719_468
        val era = floorDiv(z, 146_097)
        val doe = (z - era * 146_097).toInt()                     // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365 // [0, 399]
        val y = yoe.toLong() + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)         // [0, 365]
        val mp = (5 * doy + 2) / 153                               // [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1                       // [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9                    // [1, 12]
        return Triple(if (m <= 2) y + 1 else y, m, d)
    }

    private fun daysFromCivil(y0: Long, m: Int, d: Int): Long {
        val y = if (m <= 2) y0 - 1 else y0
        val era = floorDiv(y, 400)
        val yoe = (y - era * 400).toInt()                          // [0, 399]
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1 // [0, 365]
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy            // [0, 146096]
        return era * 146_097 + doe - 719_468
    }

    /** Common-Kotlin floor division (JVM `Math.floorDiv`). */
    private fun floorDiv(a: Long, b: Long): Long {
        val q = a / b
        return if (a % b != 0L && (a < 0) != (b < 0)) q - 1 else q
    }
}
