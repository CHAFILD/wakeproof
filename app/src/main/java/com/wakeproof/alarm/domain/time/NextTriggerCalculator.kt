package com.wakeproof.alarm.domain.time

import com.wakeproof.alarm.domain.model.Alarm
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Calculates the next moment an alarm fires, based on its days-of-week
 * set (spec 4.1.1 list card "fires in Xh Ym"). Pure java.time so it is
 * unit-testable and reusable by the scheduling stage.
 */
object NextTriggerCalculator {

    /** Maximum look-ahead: 7 days covers every possible day-of-week set. */
    private const val MAX_DAYS_AHEAD = 7

    /**
     * Returns the next local date-time the alarm fires, or null when the
     * alarm is disabled. An empty days-of-week set means "every day".
     */
    fun nextTriggerDateTime(alarm: Alarm, now: LocalDateTime): LocalDateTime? {
        if (!alarm.isEnabled) return null
        val time = LocalTime.of(alarm.hour, alarm.minute)

        val today = now.toLocalDate()
        val triggerToday = LocalDateTime.of(today, time)
        val firesToday = alarm.daysOfWeek.isEmpty() || today.dayOfWeek in alarm.daysOfWeek
        if (firesToday && triggerToday.isAfter(now)) return triggerToday

        for (offset in 1..MAX_DAYS_AHEAD) {
            val day = today.plusDays(offset.toLong())
            if (alarm.daysOfWeek.isEmpty() || day.dayOfWeek in alarm.daysOfWeek) {
                return LocalDateTime.of(day, time)
            }
        }
        // Unreachable: within 7 days every day-of-week combination occurs.
        return null
    }

    /** Minutes from [now] until the next trigger; null for disabled alarms. */
    fun minutesUntilTrigger(alarm: Alarm, now: LocalDateTime): Long? {
        val trigger = nextTriggerDateTime(alarm, now) ?: return null
        return Duration.between(now, trigger).toMinutes()
    }
}
