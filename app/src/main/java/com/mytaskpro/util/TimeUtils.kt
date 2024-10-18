package com.mytaskpro.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private var use24HourFormat: Boolean = false

    fun setUse24HourFormat(use24Hour: Boolean) {
        use24HourFormat = use24Hour
    }

    fun formatTime(time: LocalTime): String {
        val pattern = if (use24HourFormat) "HH:mm" else "hh:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return time.format(formatter)
    }

    fun formatTimeString(timeString: String): String {
        val time = LocalTime.parse(timeString)
        return formatTime(time)
    }
}