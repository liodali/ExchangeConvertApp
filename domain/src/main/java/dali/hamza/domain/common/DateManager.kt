package dali.hamza.domain.common

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object DateManager {

    private var locale = Locale.getDefault()
    val dateFormat_full = SimpleDateFormat("MMMM dd,yyyy HH:mm", locale)
    private val dateFormat_api = SimpleDateFormat("yyyy-MM-dd HH:mm", locale)




    fun setDate(year: Int, month: Int, day: Int): Date {

        val calendar = Calendar.getInstance()

        calendar[Calendar.DAY_OF_MONTH] = day

        calendar[Calendar.MONTH] = month

        calendar[Calendar.YEAR] = year
        return calendar.time
    }

    fun now(): Date {
        val calendar = Calendar.getInstance()
        return calendar.time
    }

    fun difference2Date(d1: Date, d2: Date = now()): DateExpiration {
        val diff: Long = d2.time - d1.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return DateExpiration(
            days = days.toInt(),
            hours = hours.toInt(),
            minutes = minutes.toInt(),
            seconds = seconds.toInt(),
        )

    }

}

data class DateExpiration(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
)