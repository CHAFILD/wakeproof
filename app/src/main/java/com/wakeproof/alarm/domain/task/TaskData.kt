package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.TaskType

/**
 * The four task colors used by both MEMORY and STROOP tasks (spec 4.2.1).
 * Domain stays free of Android resources: the UI layer maps each entry to
 * its Russian label (КРАСНЫЙ, ЗЕЛЁНЫЙ, СИНИЙ, ЖЁЛТЫЙ) via strings.xml.
 */
enum class TaskColor {
    RED,
    GREEN,
    BLUE,
    YELLOW;

    companion object {
        val COUNT = entries.size
    }
}

/**
 * One atomic answer step inside a task (spec 1.5 glossary: "один пример,
 * одна последовательность, один раунд Stroop"). A full task block consists
 * of several rounds; the round count per difficulty is defined by
 * [TaskDifficultyParams].
 */
sealed class TaskData {
    /** Task type this piece of data belongs to. */
    abstract val type: TaskType

    /** Single arithmetic expression, spec 4.2.1. */
    data class MathTask(
        /** Human-readable expression, e.g. "17 + 4", "23 − 8", "12 × 7". */
        val expression: String,
        /** Integer, non-negative answer (spec 4.2.1). */
        val answer: Int
    ) : TaskData() {
        override val type: TaskType get() = TaskType.MATH
    }

    /** Sequence of colored buttons the user must repeat, spec 4.2.1. */
    data class MemorySequence(
        /** Flash order of the buttons; length follows the difficulty. */
        val colors: List<TaskColor>
    ) : TaskData() {
        override val type: TaskType get() = TaskType.MEMORY
    }

    /** One Stroop round: press the INK color, not the word, spec 4.2.1. */
    data class StroopRound(
        /** Color whose Russian name is printed as the word. */
        val wordColor: TaskColor,
        /** Color of the ink the word is printed with; the correct answer. */
        val inkColor: TaskColor
    ) : TaskData() {
        override val type: TaskType get() = TaskType.STROOP
    }
}

/**
 * Outcome of one solved task block, spec 4.2.1.
 */
data class TaskResult(
    /** Wall-clock time the user spent solving the whole task block. */
    val timeTakenMs: Long,
    /** Number of wrong answers inside the block. */
    val mistakes: Int,
    val taskType: TaskType
)
