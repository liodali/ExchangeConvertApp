package dali.hamza.shared.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DateUtilsTest {

    @Test
    fun epochDayOfMillisIsUtcFloorDivision() {
        assertEquals(0L, DateUtils.epochDayOf(0L))
        assertEquals(0L, DateUtils.epochDayOf(86_400_000L - 1))
        assertEquals(1L, DateUtils.epochDayOf(86_400_000L))
        // negative times floor correctly (pre-1970)
        assertEquals(-1L, DateUtils.epochDayOf(-1L))
    }

    @Test
    fun isoDateRoundTrip() {
        for (iso in listOf("1970-01-01", "2000-02-29", "2026-10-01", "2024-12-31")) {
            val epochDay = DateUtils.epochDayOf(iso)!!
            assertEquals(iso, DateUtils.toIsoDate(epochDay), "round-trip failed for $iso")
        }
    }

    @Test
    fun knownEpochDays() {
        assertEquals(0L, DateUtils.epochDayOf("1970-01-01")!!)
        // 56 years × 365 + 14 leap days + 273 days (Jan 1 → Oct 1) = 20727
        assertEquals(20727L, DateUtils.epochDayOf("2026-10-01")!!)
    }

    @Test
    fun malformedIsoDatesReturnNull() {
        assertNull(DateUtils.epochDayOf("2026-10"))
        assertNull(DateUtils.epochDayOf("not-a-date"))
        assertNull(DateUtils.epochDayOf("2026-13-01"))
        assertNull(DateUtils.epochDayOf("2026-10-99"))
    }

    @Test
    fun formatDateTimeUsesUtc() {
        // 2026-10-01T15:30 UTC → "2026-10-01 15:30"
        val epochDay = DateUtils.epochDayOf("2026-10-01")!!
        val millis = epochDay * 86_400_000L + ((15L * 3600 + 30 * 60) * 1000)
        assertEquals("2026-10-01 15:30", DateUtils.formatDateTime(millis))
    }

    @Test
    fun isoDayMonthBoundaries() {
        assertTrue(DateUtils.epochDayOf("2026-03-01")!! > DateUtils.epochDayOf("2026-02-28")!!)
        assertTrue(DateUtils.epochDayOf("2025-01-01")!! > DateUtils.epochDayOf("2024-12-31")!!)
    }
}
