package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.TaskType

/**
 * Builds the queue of cognitive tasks for one ringing session, spec 4.2.2:
 *
 * - 1-4 tasks per session (configured per alarm, clamped defensively);
 * - task types repeat in a circle when there are more tasks than types;
 * - the same type never appears twice in a row (guaranteed by round-robin
 *   whenever two or more types are selected; with a single selected type
 *   the whole queue is that type, which the spec's circle rule reduces to);
 * - concrete task data comes from the difficulty-parameterized generators.
 */
class SessionTaskPlanner(
    private val random: RandomSource = KotlinRandomSource()
) {

    private val mathGenerator = MathTaskGenerator(random)
    private val memoryGenerator = MemoryTaskGenerator(random)
    private val stroopGenerator = StroopTaskGenerator(random)

    /**
     * Plans the whole session queue.
     *
     * @param selectedTypes task types enabled on the alarm, spec 4.1.1
     *   multi-select; duplicates are ignored, encounter order defines the
     *   round-robin circle. Must not be empty.
     * @param taskCount configured number of tasks, spec 4.2.2 range 1-4;
     *   out-of-range values are clamped.
     * @param difficulty difficulty mode of the alarm, spec 4.2.1.
     */
    fun planSession(
        selectedTypes: Collection<TaskType>,
        taskCount: Int,
        difficulty: Difficulty
    ): List<TaskData> {
        require(selectedTypes.isNotEmpty()) {
            "At least one task type must be selected (spec 4.1.1 multi-select)."
        }
        val types = selectedTypes.distinct()
        val count = taskCount.coerceIn(Alarm.TASK_COUNT_MIN, Alarm.TASK_COUNT_MAX)
        return List(count) { index ->
            when (val type = types[index % types.size]) {
                TaskType.MATH -> mathGenerator.generate(difficulty)
                TaskType.MEMORY -> memoryGenerator.generate(difficulty)
                TaskType.STROOP -> stroopGenerator.generate(difficulty)
            }
        }
    }
}
