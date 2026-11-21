package com.wakeproof.alarm.domain.scheduler

import com.wakeproof.alarm.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope

/**
 * Schedules actual alarm firing with AlarmManager.setAlarmClock()
 * (spec section 2: exact alarms, Doze-proof, status-bar alarm icon).
 *
 * The Room database is the single source of truth: enabled alarms are
 * mirrored into AlarmManager, disabled/deleted alarms are cancelled.
 */
interface AlarmScheduler {

    /**
     * Schedules the next occurrence of the alarm (weekday set, midnight
     * rollover — spec 4.1.1). Replaces any previous schedule for the
     * same alarm id. A disabled alarm is cancelled instead.
     */
    fun schedule(alarm: Alarm)

    /** Cancels the pending alarm with the given id (disable/delete). */
    fun cancel(alarmId: Long)

    /**
     * Re-reads all alarms from Room and mirrors them into AlarmManager
     * (boot completed, timezone/time changed — spec section 5).
     */
    suspend fun rescheduleAllEnabled()

    /**
     * Observes the alarm table while the process is alive and keeps the
     * AlarmManager in sync on every create/edit/delete/toggle, so no UI
     * code has to call the scheduler manually.
     */
    fun startAutoSync(scope: CoroutineScope)
}
