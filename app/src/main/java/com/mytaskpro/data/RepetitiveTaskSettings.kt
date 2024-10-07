package com.mytaskpro.data

import com.mytaskpro.ui.EndOption
import com.mytaskpro.ui.RepetitionType
import java.time.LocalDate

data class RepetitiveTaskSettings(
    val type: RepetitionType = RepetitionType.ONE_TIME,
    val interval: Int = 1,
    val weekDays: List<Int> = emptyList(),
    val monthDay: Int? = null,
    val monthWeek: Int? = null,
    val monthWeekDay: Int? = null,
    val endOption: EndOption = EndOption.NEVER,
    val endDate: LocalDate? = null,
    val endOccurrences: Int? = null
)