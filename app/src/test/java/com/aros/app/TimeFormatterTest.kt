package com.aros.app

import com.aros.app.core.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeFormatterTest {

    @Test
    fun `formatMinuteOfDay 12-hour format handles midnight and morning edge cases`() {
        assertEquals("12:00 AM", TimeFormatter.formatMinuteOfDay(0, use24Hour = false))
        assertEquals("12:01 AM", TimeFormatter.formatMinuteOfDay(1, use24Hour = false))
        assertEquals("12:30 AM", TimeFormatter.formatMinuteOfDay(30, use24Hour = false))
        assertEquals("1:00 AM", TimeFormatter.formatMinuteOfDay(60, use24Hour = false))
        assertEquals("11:59 AM", TimeFormatter.formatMinuteOfDay(719, use24Hour = false))
    }

    @Test
    fun `formatMinuteOfDay 12-hour format handles noon and evening edge cases`() {
        assertEquals("12:00 PM", TimeFormatter.formatMinuteOfDay(720, use24Hour = false))
        assertEquals("12:30 PM", TimeFormatter.formatMinuteOfDay(750, use24Hour = false))
        assertEquals("1:00 PM", TimeFormatter.formatMinuteOfDay(780, use24Hour = false))
        assertEquals("7:30 PM", TimeFormatter.formatMinuteOfDay(1170, use24Hour = false))
        assertEquals("11:59 PM", TimeFormatter.formatMinuteOfDay(1439, use24Hour = false))
    }

    @Test
    fun `formatMinuteOfDay 24-hour format works correctly`() {
        assertEquals("00:00", TimeFormatter.formatMinuteOfDay(0, use24Hour = true))
        assertEquals("00:30", TimeFormatter.formatMinuteOfDay(30, use24Hour = true))
        assertEquals("12:00", TimeFormatter.formatMinuteOfDay(720, use24Hour = true))
        assertEquals("19:30", TimeFormatter.formatMinuteOfDay(1170, use24Hour = true))
        assertEquals("23:59", TimeFormatter.formatMinuteOfDay(1439, use24Hour = true))
    }

    @Test
    fun `formatMinuteOfDay handles overflow and negative values cleanly`() {
        // -60 minutes = 23:00 (11:00 PM)
        assertEquals("11:00 PM", TimeFormatter.formatMinuteOfDay(-60, use24Hour = false))
        assertEquals("23:00", TimeFormatter.formatMinuteOfDay(-60, use24Hour = true))

        // 1440 minutes = 00:00 (12:00 AM)
        assertEquals("12:00 AM", TimeFormatter.formatMinuteOfDay(1440, use24Hour = false))
        assertEquals("00:00", TimeFormatter.formatMinuteOfDay(1440, use24Hour = true))
    }

    @Test
    fun `formatClock and formatDateTime include AM PM in 12-hour mode`() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 8, 14, 30, 45)
        }
        val millis = calendar.timeInMillis

        val formatted12 = TimeFormatter.formatClock(millis, includeSeconds = false, use24Hour = false)
        assertTrue(formatted12.endsWith("AM") || formatted12.endsWith("PM"))

        val formattedDateTime12 = TimeFormatter.formatDateTime(millis, use24Hour = false)
        assertTrue(formattedDateTime12.endsWith("AM") || formattedDateTime12.endsWith("PM"))

        val formattedDateTime24 = TimeFormatter.formatDateTime(millis, use24Hour = true)
        assertTrue(!formattedDateTime24.endsWith("AM") && !formattedDateTime24.endsWith("PM"))
    }
}
