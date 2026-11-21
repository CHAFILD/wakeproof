package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Spec 4.2.1 parameter table: one exact row per difficulty, used by every
 * generator and by the future ringing-session stage.
 */
class TaskDifficultyParamsTest {

    @Test
    fun `easy row matches the spec table`() {
        val params = TaskDifficultyParams.forDifficulty(Difficulty.EASY)
        assertEquals(2, params.mathAnswersInRow)
        assertEquals(20, params.mathMaxOperand)
        assertEquals(10, params.mathMaxMultiplyOperand)
        assertEquals(4, params.memorySequenceLength)
        assertEquals(3, params.stroopRounds)
    }

    @Test
    fun `medium row matches the spec table`() {
        val params = TaskDifficultyParams.forDifficulty(Difficulty.MEDIUM)
        assertEquals(3, params.mathAnswersInRow)
        assertEquals(99, params.mathMaxOperand)
        assertEquals(15, params.mathMaxMultiplyOperand)
        assertEquals(5, params.memorySequenceLength)
        assertEquals(5, params.stroopRounds)
    }

    @Test
    fun `hardcore row matches the spec table`() {
        val params = TaskDifficultyParams.forDifficulty(Difficulty.HARDCORE)
        assertEquals(5, params.mathAnswersInRow)
        assertEquals(999, params.mathMaxOperand)
        assertEquals(30, params.mathMaxMultiplyOperand)
        assertEquals(6, params.memorySequenceLength)
        assertEquals(7, params.stroopRounds)
    }

    @Test
    fun `math operands never exceed the multiplication bound`() {
        // Sanity: the multiply bound is always tighter than the general
        // operand bound, otherwise multiplication could overflow the range.
        for (difficulty in Difficulty.entries) {
            val params = TaskDifficultyParams.forDifficulty(difficulty)
            assertTrue(params.mathMaxMultiplyOperand <= params.mathMaxOperand)
        }
    }
}
