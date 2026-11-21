package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty

/**
 * Per-difficulty task parameters, spec 4.2.1 table. Single source of truth
 * for all generators; the ringing-session stage reads the round counts
 * from here instead of hardcoding them again.
 *
 * | Parameter            | Easy | Medium | Hardcore |
 * |----------------------|------|--------|----------|
 * | Math answers in row  | 2    | 3      | 5        |
 * | Math operands up to  | 20   | 99     | 999      |
 * | Math multiply up to  | 10x10| 15x15  | 30x30    |
 * | Memory sequence len  | 4    | 5      | 6        |
 * | Stroop rounds        | 3    | 5      | 7        |
 */
data class TaskParams(
    /** Correct answers in a row required to finish one math task. */
    val mathAnswersInRow: Int,
    /** Upper bound (inclusive) for addition/subtraction operands. */
    val mathMaxOperand: Int,
    /** Upper bound (inclusive) for each multiplication operand. */
    val mathMaxMultiplyOperand: Int,
    /** Length of one memory sequence. */
    val memorySequenceLength: Int,
    /** Stroop rounds in a row inside one task. */
    val stroopRounds: Int
)

/**
 * The spec 4.2.1 parameter table.
 */
object TaskDifficultyParams {

    fun forDifficulty(difficulty: Difficulty): TaskParams = when (difficulty) {
        Difficulty.EASY -> TaskParams(
            mathAnswersInRow = 2,
            mathMaxOperand = 20,
            mathMaxMultiplyOperand = 10,
            memorySequenceLength = 4,
            stroopRounds = 3
        )
        Difficulty.MEDIUM -> TaskParams(
            mathAnswersInRow = 3,
            mathMaxOperand = 99,
            mathMaxMultiplyOperand = 15,
            memorySequenceLength = 5,
            stroopRounds = 5
        )
        Difficulty.HARDCORE -> TaskParams(
            mathAnswersInRow = 5,
            mathMaxOperand = 999,
            mathMaxMultiplyOperand = 30,
            memorySequenceLength = 6,
            stroopRounds = 7
        )
    }
}
