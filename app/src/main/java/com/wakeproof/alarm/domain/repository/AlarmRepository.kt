package com.wakeproof.alarm.domain.repository

import com.wakeproof.alarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

/**
 * Single source of alarms for the UI and future scheduling stage.
 * Implemented in the data layer on top of Room.
 */
interface AlarmRepository {

    /** Emits the full alarm list on every table change. */
    fun observeAlarms(): Flow<List<Alarm>>

    /** Returns the alarm by id or null when it does not exist. */
    suspend fun getAlarmById(id: Long): Alarm?

    /** Inserts a new alarm (id = 0) or updates an existing one. */
    suspend fun upsert(alarm: Alarm): Long

    /** Deletes the alarm by id. */
    suspend fun deleteAlarmById(id: Long)

    /** Enables or disables the alarm without rewriting its other fields. */
    suspend fun setEnabled(id: Long, enabled: Boolean)

    /**
     * Partial update of the snooze-state columns (spec 4.1.2) from the
     * given alarm: pending target, limit flag and the snoozed-session
     * parameters/link. Other alarm fields are not touched.
     */
    suspend fun updateSnoozeState(alarm: Alarm)
}
