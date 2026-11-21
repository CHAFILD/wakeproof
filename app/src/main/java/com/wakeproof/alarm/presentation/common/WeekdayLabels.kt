package com.wakeproof.alarm.presentation.common

import com.wakeproof.alarm.R
import java.time.DayOfWeek

/**
 * Short Russian weekday labels (Пн..Вс) shared by the alarm list and the
 * editor. All user-facing strings come from strings.xml only.
 */
fun weekdayShortLabelRes(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.day_short_mon
    DayOfWeek.TUESDAY -> R.string.day_short_tue
    DayOfWeek.WEDNESDAY -> R.string.day_short_wed
    DayOfWeek.THURSDAY -> R.string.day_short_thu
    DayOfWeek.FRIDAY -> R.string.day_short_fri
    DayOfWeek.SATURDAY -> R.string.day_short_sat
    DayOfWeek.SUNDAY -> R.string.day_short_sun
}
