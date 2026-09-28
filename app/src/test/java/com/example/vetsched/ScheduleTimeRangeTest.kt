package com.example.vetsched

import com.example.vetsched.ui.ScheduleTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ScheduleTimeRangeTest {

    @Test
    fun parsesDatabaseTimeAndFormatsForDisplay() {
        val range = ScheduleTimeRange.parse("13:00:00 - 15:00:00")

        assertEquals(13 * 60, range?.startMinutes)
        assertEquals(15 * 60, range?.endMinutes)
        assertEquals("1:00 PM - 3:00 PM", ScheduleTimeRange.format("13:00:00 - 15:00:00", Locale.US))
    }

    @Test
    fun preservesHalfHourPositionAndDuration() {
        val range = ScheduleTimeRange.parse("7:30 AM - 8:00 AM")

        assertEquals(7 * 60 + 30, range?.startMinutes)
        assertEquals(30, range?.durationMinutes)
    }

    @Test
    fun rejectsMalformedTimeRanges() {
        assertNull(ScheduleTimeRange.parse("TBA"))
        assertNull(ScheduleTimeRange.parse("25:00 - 26:00"))
    }
}
