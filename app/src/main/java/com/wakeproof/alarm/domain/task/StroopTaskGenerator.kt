package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty

/**
 * Generates single Stroop rounds, spec 4.2.1: a color name printed with a
 * DIFFERENT ink color; the correct answer is always the ink color.
 * Rounds per task block come from [TaskParams.stroopRounds].
 */
class StroopTaskGenerator(
    private val random: RandomSource
) {

    fun generate(difficulty: Difficulty): TaskData.StroopRound =
        generate(TaskDifficultyParams.forDifficulty(difficulty))

    fun generate(params: TaskParams): TaskData.StroopRound {
        val word = TaskColor.entries[random.nextInt(0, TaskColor.COUNT)]
        var ink = TaskColor.entries[random.nextInt(0, TaskColor.COUNT)]
        // Spec 4.2.1: the word is printed in a DIFFERENT ink color.
        while (ink == word) {
            ink = TaskColor.entries[random.nextInt(0, TaskColor.COUNT)]
        }
        return TaskData.StroopRound(wordColor = word, inkColor = ink)
    }
}
