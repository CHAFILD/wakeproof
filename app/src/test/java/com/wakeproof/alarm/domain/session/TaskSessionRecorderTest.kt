package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.task.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TaskSessionRecorder (spec 4.1.4 / 6): one session row per ringing,
 * CLEAN and EMERGENCY results, one-shot finish, duration from the
 * injected clock, mistakes of unfinished blocks included.
 */
class TaskSessionRecorderTest {

    private class FakeRepository : TaskSessionRepository {
        val saved = mutableListOf<TaskSession>()
        private val flow = MutableStateFlow<List<TaskSession>>(emptyList())

        override suspend fun saveSession(session: TaskSession): Long {
            saved += session
            return saved.size.toLong()
        }

        override fun observeSessions(): Flow<List<TaskSession>> = flow
    }

    private class MutableClock(var now: Long = 100_000L) {
        val read: () -> Long = { now }
    }

    private val results = listOf(
        TaskResult(timeTakenMs = 4_000L, mistakes = 1, taskType = TaskType.MATH),
        TaskResult(timeTakenMs = 8_000L, mistakes = 0, taskType = TaskType.STROOP)
    )

    @Test
    fun `clean finish writes one row with all fields`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 7L, repository = repository, clock = clock.read)
        clock.now += 25_000L
        val session = recorder.finish(TaskSessionResult.CLEAN, results, totalMistakes = 1)

        assertNotNull(session)
        val stored = repository.saved.single()
        assertEquals(7L, stored.alarmId)
        assertEquals(100_000L, stored.startedAt)
        assertEquals(125_000L, stored.finishedAt)
        assertEquals(25_000L, stored.durationMs)
        assertEquals(TaskSessionResult.CLEAN, stored.result)
        assertEquals(1, stored.mistakes)
        assertEquals(results, stored.taskResults)
        assertEquals(2, stored.solvedTaskCount)
    }

    @Test
    fun `emergency finish records solved tasks and current block mistakes`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 3L, repository = repository, clock = clock.read)
        clock.now += 12_000L
        // One solved block + two mistakes inside the unfinished block.
        val session = recorder.finish(
            TaskSessionResult.EMERGENCY,
            taskResults = results.take(1),
            totalMistakes = 2
        )

        assertNotNull(session)
        val stored = repository.saved.single()
        assertEquals(TaskSessionResult.EMERGENCY, stored.result)
        assertEquals(1, stored.solvedTaskCount)
        assertEquals(2, stored.mistakes, "Mistakes of the unfinished block must be kept")
        assertEquals(12_000L, stored.durationMs)
    }

    @Test
    fun `finish is one-shot - second call writes nothing`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 1L, repository = repository, clock = clock.read)
        assertNotNull(recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0))
        assertNull(recorder.finish(TaskSessionResult.EMERGENCY, emptyList(), 0))
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `duration never goes negative with a backwards clock`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock(10_000L)
        val recorder = TaskSessionRecorder(alarmId = 1L, repository = repository, clock = clock.read)
        clock.now = 4_000L
        val session = recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0)
        assertNotNull(session)
        assertTrue(repository.saved.single().durationMs >= 0)
    }
}
