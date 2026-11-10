package com.aros.app.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    /**
     * Formats a minute-of-day integer (0..1439) into a time string.
     * When use24Hour is false (default): "7:30 PM", "12:00 AM", "12:30 PM"
     * When use24Hour is true: "19:30", "00:00", "12:30"
     */
    fun formatMinuteOfDay(minuteOfDay: Int, use24Hour: Boolean = false): String {
        val totalMinutes = ((minuteOfDay % 1440) + 1440) % 1440
        val hour = totalMinutes / 60
        val minute = totalMinutes % 60

        return if (use24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when (hour) {
                0 -> 12
                in 1..12 -> hour
                else -> hour - 12
            }
            String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
        }
    }

    /**
     * Formats an epoch timestamp (milliseconds) into clock time.
     * When use24Hour is false (default): "7:30 PM" (or with seconds: "7:30:12 PM")
     * When use24Hour is true: "19:30" (or with seconds: "19:30:12")
     */
    fun formatClock(timestampMillis: Long, includeSeconds: Boolean = false, use24Hour: Boolean = false): String {
        val pattern = when {
            use24Hour && includeSeconds -> "HH:mm:ss"
            use24Hour && !includeSeconds -> "HH:mm"
            !use24Hour && includeSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
    }

    /**
     * Formats a timestamp into date + clock time string for audit logs.
     * When use24Hour is false (default): "Nov 07, 7:30:12 PM"
     * When use24Hour is true: "Nov 07, 19:30:12"
     */
    fun formatDateTime(timestampMillis: Long, use24Hour: Boolean = false): String {
        val pattern = if (use24Hour) "MMM dd, HH:mm:ss" else "MMM dd, h:mm:ss a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
    }
}
