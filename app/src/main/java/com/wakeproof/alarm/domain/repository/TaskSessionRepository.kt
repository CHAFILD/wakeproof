package com.wakeproof.alarm.domain.repository

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.task.TaskResult
import kotlinx.coroutines.flow.Flow

/**
 * Persistence of ringing sessions (task_sessions table, spec 6). The
 * statistics stage reads them; sessions are written by TaskSessionRecorder.
 */
interface TaskSessionRepository {

    suspend fun saveSession(session: TaskSession): Long

    /** Emits every session, newest first. */
    fun observeSessions(): Flow<List<TaskSession>>

    /** Snapshot of the whole history, oldest first (streak calculations). */
    suspend fun getAllChronological(): List<TaskSession>

    /**
     * Continues an already persisted morning session (the snoozed ring
     * belongs to the same morning, spec 1.5 / 4.4): appends task results
     * and mistakes, moves the finish moment, and KEEPS the stored result
     * (a snoozed morning stays SNOOZED even when the repeated ring is
     * solved). Returns the merged row or null when it no longer exists.
     */
    suspend fun continueSession(
        sessionId: Long,
        extraTaskResults: List<TaskResult>,
        extraMistakes: Int,
        finishedAtMillis: Long
    ): TaskSession?
}
