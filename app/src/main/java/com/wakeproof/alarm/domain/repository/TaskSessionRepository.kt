package com.wakeproof.alarm.domain.repository

import com.wakeproof.alarm.domain.model.TaskSession
import kotlinx.coroutines.flow.Flow

/**
 * Persistence of ringing sessions (task_sessions table, spec 6). The
 * statistics stage reads them; this stage only writes and exposes a
 * debug/inspection flow.
 */
interface TaskSessionRepository {

    suspend fun saveSession(session: TaskSession): Long

    /** Emits every session, newest first. */
    fun observeSessions(): Flow<List<TaskSession>>
}
