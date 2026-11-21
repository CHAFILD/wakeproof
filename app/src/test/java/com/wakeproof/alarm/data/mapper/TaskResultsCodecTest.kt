package com.wakeproof.alarm.data.mapper

import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.task.TaskResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TaskResultsCodec: lossless round trip, empty input, and defensive
 * decoding of unknown/corrupt entries (never crashes the reader).
 */
class TaskResultsCodecTest {

    private val results = listOf(
        TaskResult(timeTakenMs = 12_500L, mistakes = 1, taskType = TaskType.MATH),
        TaskResult(timeTakenMs = 8_000L, mistakes = 0, taskType = TaskType.STROOP),
        TaskResult(timeTakenMs = 21_000L, mistakes = 3, taskType = TaskType.MEMORY)
    )

    @Test
    fun `round trip is lossless`() {
        assertEquals(results, TaskResultsCodec.decode(TaskResultsCodec.encode(results)))
    }

    @Test
    fun `empty list encodes to blank and back`() {
        assertEquals("", TaskResultsCodec.encode(emptyList()))
        assertEquals(emptyList<TaskResult>(), TaskResultsCodec.decode(""))
        assertEquals(emptyList<TaskResult>(), TaskResultsCodec.decode("  "))
    }

    @Test
    fun `encoded format is the documented pipe-semicolon shape`() {
        assertEquals(
            "MATH|12500|1;STROOP|8000|0;MEMORY|21000|3",
            TaskResultsCodec.encode(results)
        )
    }

    @Test
    fun `unknown task type is skipped instead of crashing`() {
        val decoded = TaskResultsCodec.decode("MATH|100|0;SOMETHING_NEW|200|2")
        assertEquals(1, decoded.size)
        assertEquals(TaskType.MATH, decoded.single().taskType)
    }

    @Test
    fun `corrupt entries are skipped`() {
        assertEquals(0, TaskResultsCodec.decode("garbage").size)
        assertEquals(0, TaskResultsCodec.decode("MATH|notanumber|1").size)
        assertEquals(0, TaskResultsCodec.decode("MATH|100").size)
        assertTrue(TaskResultsCodec.decode("MATH|100|0;;STROOP|50|0").size == 2)
    }
}
