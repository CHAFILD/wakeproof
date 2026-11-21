package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty

/**
 * Generates single arithmetic expressions, spec 4.2.1:
 * addition, subtraction and multiplication only; integer, non-negative
 * answers; operand bounds follow the difficulty table.
 *
 * Expression format is "a + b", "a − b" (U+2212) and "a × b" (U+00D7)
 * with single spaces, so the UI can render it as-is.
 */
class MathTaskGenerator(
    private val random: RandomSource
) {

    /** One round (one expression) for the given difficulty. */
    fun generate(difficulty: Difficulty): TaskData.MathTask =
        generate(TaskDifficultyParams.forDifficulty(difficulty))

    /** One round (one expression) for explicit parameters. */
    fun generate(params: TaskParams): TaskData.MathTask {
        return when (random.nextInt(0, OPERATIONS)) {
            OP_ADD -> {
                val a = random.nextInt(1, params.mathMaxOperand + 1)
                val b = random.nextInt(1, params.mathMaxOperand + 1)
                TaskData.MathTask("$a + $b", a + b)
            }
            OP_SUBTRACT -> {
                // First operand >= second keeps the answer non-negative.
                val a = random.nextInt(1, params.mathMaxOperand + 1)
                val b = random.nextInt(1, a + 1)
                TaskData.MathTask("$a − $b", a - b)
            }
            else -> {
                val a = random.nextInt(1, params.mathMaxMultiplyOperand + 1)
                val b = random.nextInt(1, params.mathMaxMultiplyOperand + 1)
                TaskData.MathTask("$a × $b", a * b)
            }
        }
    }

    private companion object {
        const val OPERATIONS = 3
        const val OP_ADD = 0
        const val OP_SUBTRACT = 1
    }
}
