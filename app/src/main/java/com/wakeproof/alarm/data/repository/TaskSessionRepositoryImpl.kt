package com.wakeproof.alarm.data.repository

import com.wakeproof.alarm.data.local.dao.TaskSessionDao
import com.wakeproof.alarm.data.local.entity.TaskSessionEntity
import com.wakeproof.alarm.data.mapper.TaskResultsCodec
import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.task.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [TaskSessionRepository]. Encoding of
 * the per-task results lives in [TaskResultsCodec]; unknown enum
 * names decode to a safe fallback instead of crashing.
 */
class TaskSessionRepositoryImpl(
    private val dao: TaskSessionDao
) : TaskSessionRepository {

    override suspend fun saveSession(session: TaskSession): Long =
        dao.insert(toEntity(session))

    override fun observeSessions(): Flow<List<TaskSession>> =
        dao.observeAll().map { entities -> entities.map(::toDomain) }

    override suspend fun getAllChronological(): List<TaskSession> =
        dao.getAllChronological().map(::toDomain)

    override suspend fun continueSession(
        sessionId: Long,
        extraTaskResults: List<TaskResult>,
        extraMistakes: Int,
        finishedAtMillis: Long
    ): TaskSession? {
        val existing = dao.getById(sessionId) ?: return null
        val merged = existing.copy(
            finishedAt = finishedAtMillis,
            durationMs = (finishedAtMillis - existing.startedAt).coerceAtLeast(0L),
            mistakes = existing.mistakes + extraMistakes,
            taskResults = TaskResultsCodec.encode(
                TaskResultsCodec.decode(existing.taskResults) + extraTaskResults
            )
        )
        dao.update(merged)
        return toDomain(merged)
    }

    private fun toEntity(session: TaskSession): TaskSessionEntity =
        TaskSessionEntity(
            id = session.id,
            alarmId = session.alarmId,
            startedAt = session.startedAt,
            finishedAt = session.finishedAt,
            result = session.result.name,
            mistakes = session.mistakes,
            durationMs = session.durationMs,
            taskResults = TaskResultsCodec.encode(session.taskResults)
        )

    private fun toDomain(entity: TaskSessionEntity): TaskSession =
        TaskSession(
            id = entity.id,
            alarmId = entity.alarmId,
            startedAt = entity.startedAt,
            finishedAt = entity.finishedAt,
            result = runCatching { TaskSessionResult.valueOf(entity.result) }
                .getOrDefault(TaskSessionResult.CLEAN),
            mistakes = entity.mistakes,
            durationMs = entity.durationMs,
            taskResults = TaskResultsCodec.decode(entity.taskResults)
        )
}
