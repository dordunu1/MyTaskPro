package com.mytaskpro.data

import com.mytaskpro.ui.EndOption
import com.mytaskpro.ui.RepetitionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class RepetitiveTaskSettings(
    val type: RepetitionType = RepetitionType.ONE_TIME,
    val interval: Int = 1,
    val weekDays: List<Int> = emptyList(),
    val monthDay: Int? = null,
    val monthWeek: Int? = null,
    val monthWeekDay: Int? = null,
    val endOption: EndOption = EndOption.NEVER,
    private val endDateTimestamp: Long? = null,
    val endOccurrences: Int? = null
) {
    // Convert timestamp to LocalDate when needed
    val endDate: LocalDate?
        get() = endDateTimestamp?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
        }

    // Helper function to create a copy with a new end date
    fun copyWithEndDate(newEndDate: LocalDate?): RepetitiveTaskSettings {
        return copy(
            endDateTimestamp = newEndDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
    }

    companion object {
        // Helper function to create from a LocalDate
        fun fromLocalDate(
            type: RepetitionType = RepetitionType.ONE_TIME,
            interval: Int = 1,
            weekDays: List<Int> = emptyList(),
            monthDay: Int? = null,
            monthWeek: Int? = null,
            monthWeekDay: Int? = null,
            endOption: EndOption = EndOption.NEVER,
            endDate: LocalDate? = null,
            endOccurrences: Int? = null
        ): RepetitiveTaskSettings {
            return RepetitiveTaskSettings(
                type = type,
                interval = interval,
                weekDays = weekDays,
                monthDay = monthDay,
                monthWeek = monthWeek,
                monthWeekDay = monthWeekDay,
                endOption = endOption,
                endDateTimestamp = endDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
                endOccurrences = endOccurrences
            )
        }
    }
}