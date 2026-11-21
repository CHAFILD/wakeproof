package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty

/**
 * Generates memory sequences, spec 4.2.1: repeated color flashes the user
 * must replay. Sequence length follows the difficulty table; every position
 * is an independent draw from the four colors, so repeats are allowed
 * (classic "Simon says" behavior).
 */
class MemoryTaskGenerator(
    private val random: RandomSource
) {

    fun generate(difficulty: Difficulty): TaskData.MemorySequence =
        generate(TaskDifficultyParams.forDifficulty(difficulty))

    fun generate(params: TaskParams): TaskData.MemorySequence {
        val colors = List(params.memorySequenceLength) {
            TaskColor.entries[random.nextInt(0, TaskColor.COUNT)]
        }
        return TaskData.MemorySequence(colors)
    }
}
