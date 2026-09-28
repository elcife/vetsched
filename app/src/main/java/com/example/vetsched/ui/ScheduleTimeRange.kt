package com.example.vetsched.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal data class ScheduleTimeRange(
    val startMinutes: Int,
    val endMinutes: Int
) {
    val durationMinutes: Int
        get() = endMinutes - startMinutes

    companion object {
        private val rangeSeparator = Regex("\\s*(?:-|/|•)\\s*")
        private val timePattern = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(AM|PM)?$", RegexOption.IGNORE_CASE)

        fun parse(value: String): ScheduleTimeRange? {
            val parts = value.trim().split(rangeSeparator, limit = 2)
            if (parts.size != 2) return null

            val start = parseTime(parts[0]) ?: return null
            var end = parseTime(parts[1]) ?: return null
            if (end <= start) end += 24 * 60
            return ScheduleTimeRange(start, end)
        }

        fun format(value: String, locale: Locale = Locale.getDefault()): String {
            val range = parse(value) ?: return value
            val formatter = SimpleDateFormat("h:mm a", locale)
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, range.startMinutes / 60 % 24)
                set(Calendar.MINUTE, range.startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = start.clone() as Calendar
            end.add(Calendar.MINUTE, range.durationMinutes)
            return "${formatter.format(start.time)} - ${formatter.format(end.time)}"
        }

        private fun parseTime(value: String): Int? {
            val match = timePattern.matchEntire(value.trim()) ?: return null
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            val meridiem = match.groupValues[3].uppercase(Locale.ROOT)
            if (minute !in 0..59) return null

            val hour24 = if (meridiem.isNotEmpty()) {
                if (hour !in 1..12) return null
                when {
                    meridiem == "AM" && hour == 12 -> 0
                    meridiem == "PM" && hour < 12 -> hour + 12
                    else -> hour
                }
            } else {
                if (hour !in 0..23) return null
                hour
            }
            return hour24 * 60 + minute
        }
    }
}
