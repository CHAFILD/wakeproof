package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.task.KotlinRandomSource
import com.wakeproof.alarm.domain.task.MathTaskGenerator
import com.wakeproof.alarm.domain.task.MemoryTaskGenerator
import com.wakeproof.alarm.domain.task.RandomSource
import com.wakeproof.alarm.domain.task.SessionTaskPlanner
import com.wakeproof.alarm.domain.task.StroopTaskGenerator
import com.wakeproof.alarm.domain.task.TaskColor
import com.wakeproof.alarm.domain.task.TaskData
import com.wakeproof.alarm.domain.task.TaskDifficultyParams
import com.wakeproof.alarm.domain.task.TaskResult

/**
 * Snapshot of the round currently on screen. The UI renders exactly one
 * task block at a time (spec 4.1.3: one task on screen); the engine
 * mutates this state on every answer.
 */
sealed interface RoundState {

    /** One arithmetic example; [requiredInRow] correct answers in a row. */
    data class Math(
        val expression: String,
        /** Correct answers in a row so far; resets to 0 on a mistake. */
        val solvedInRow: Int,
        val requiredInRow: Int
    ) : RoundState

    /** The color sequence round; the user repeats the flashed order. */
    data class Memory(
        /** Number of colors in the sequence (flash + repeat). */
        val sequenceLength: Int,
        /** Correct taps of the current replay attempt so far. */
        val solvedInRow: Int
    ) : RoundState

    /** One Stroop round: press the INK color of the printed word. */
    data class Stroop(
        val wordColor: TaskColor,
        val inkColor: TaskColor,
        /** Rounds solved in a row so far; resets to 0 on a mistake. */
        val solvedInRow: Int,
        val requiredInRow: Int
    ) : RoundState
}

/** Immutable state the UI observes. */
data class EngineState(
    /** 0-based index of the current task block. */
    val taskIndex: Int,
    val totalTasks: Int,
    /** Wrong answers across the whole session so far. */
    val totalMistakes: Int,
    /** True after the last required block was solved. */
    val isFinished: Boolean,
    val round: RoundState
)

/** Result of one submitted answer. */
enum class AnswerOutcome {
    /** Correct, but the block needs more rounds. */
    CORRECT,

    /** Correct and the whole task block is done. */
    BLOCK_DONE,

    /** Wrong answer: streak reset (math/Stroop) or memory replay. */
    MISTAKE
}

/**
 * Pure state machine of one ringing session's task queue, spec 4.2:
 *
 * - the block queue (types + count) comes from [SessionTaskPlanner]
 *   (round-robin, no same type twice in a row);
 * - MATH needs [TaskDifficultyParams.mathAnswersInRow] correct answers
 *   in a row; a mistake resets the streak and shows a NEW example
 *   (so brute-forcing one example does not work);
 * - MEMORY shows one sequence; every tap is checked against the exact
 *   position; a wrong tap replays the SAME sequence (the user must be
 *   able to learn it - no dead ends, spec 1.4.4);
 * - STROOP needs [TaskDifficultyParams.stroopRounds] correct rounds in
 *   a row; a mistake resets the streak with a new round;
 * - finished blocks are recorded as [TaskResult] for the session log.
 *
 * No Android dependencies; time comes from an injectable clock so unit
 * tests are fully deterministic.
 */
class TaskSessionEngine(
    selectedTypes: Collection<TaskType>,
    taskCount: Int,
    private val difficulty: Difficulty,
    random: RandomSource = KotlinRandomSource(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    init {
        require(selectedTypes.isNotEmpty()) {
            "At least one task type must be selected (spec 4.1.1)."
        }
    }

    private val mathGenerator = MathTaskGenerator(random)
    private val memoryGenerator = MemoryTaskGenerator(random)
    private val stroopGenerator = StroopTaskGenerator(random)
    private val params = TaskDifficultyParams.forDifficulty(difficulty)

    /**
     * The planned queue of block types. SessionTaskPlanner returns one
     * round per planned block; only the round-robin of TYPES is used
     * here, rounds are generated lazily per block.
     */
    private val blockTypes: List<TaskType> =
        SessionTaskPlanner(random).planSession(selectedTypes, taskCount, difficulty)
            .map { it.type }

    private val recordedResults = mutableListOf<TaskResult>()

    /** Wall clock when the current block started (for TaskResult.timeTakenMs). */
    private var blockStartedAt = clock()

    /** Wrong answers inside the current block (for TaskResult.mistakes). */
    private var blockMistakes = 0

    /**
     * Current MEMORY sequence; stable across replays so a wrong tap
     * replays the same order.
     */
    private var memorySequence: TaskData.MemorySequence? = null

    /** Number of correct memory taps in the current attempt. */
    private var memoryPosition = 0

    private var mathStreak = 0
    private var stroopStreak = 0

    /**
     * Current math example. Declared BEFORE the init block: its
     * initializer must not overwrite the example generated by
     * startBlock(0) (property initializers run in declaration order).
     */
    private var currentMathData: TaskData.MathTask = mathGenerator.generate(difficulty)

    private var _state: EngineState = EngineState(
        taskIndex = 0,
        totalTasks = blockTypes.size,
        totalMistakes = 0,
        isFinished = false,
        round = RoundState.Memory(0, solvedInRow = 0) // replaced immediately in startFirstBlock
    )

    val state: EngineState get() = _state

    /** True after the last required block was solved. */
    val isFinished: Boolean get() = _state.isFinished

    /** Results of the fully solved blocks, in order. */
    val results: List<TaskResult> get() = recordedResults.toList()

    init {
        startBlock(0)
    }

    // --- MATH ---------------------------------------------------------------

    /**
     * Submits the typed answer for the current math example. A wrong
     * answer resets the in-task streak and generates a new example.
     */
    fun onMathSubmit(answer: Int): AnswerOutcome {
        val round = _state.round as? RoundState.Math
            ?: throw IllegalStateException("Current round is not MATH")
        val correct = currentMathData.answer == answer
        return if (correct) {
            mathStreak += 1
            if (mathStreak >= round.requiredInRow) {
                completeBlock(TaskType.MATH)
                AnswerOutcome.BLOCK_DONE
            } else {
                nextMathRound()
                AnswerOutcome.CORRECT
            }
        } else {
            registerMistake()
            mathStreak = 0
            nextMathRound()
            AnswerOutcome.MISTAKE
        }
    }

    // --- MEMORY -------------------------------------------------------------

    /** The sequence to flash for the current memory block. */
    fun memorySequenceColors(): List<TaskColor> {
        val round = _state.round as? RoundState.Memory
            ?: throw IllegalStateException("Current round is not MEMORY")
        val sequence = memorySequence
            ?: memoryGenerator.generate(difficulty).also { memorySequence = it }
        check(sequence.colors.size == round.sequenceLength) {
            "Generated sequence length does not match the difficulty table"
        }
        return sequence.colors
    }

    /**
     * Checks one tap of the user's replay at [position] (0-based).
     * A wrong tap replays the same sequence: [AnswerOutcome.MISTAKE]
     * and the UI restarts the flash animation from position 0.
     */
    fun onMemoryTap(position: Int, color: TaskColor): AnswerOutcome {
        check(memorySequence != null) { "memorySequenceColors() must be called first" }
        val sequence = memorySequence!!
        val round = _state.round as? RoundState.Memory
            ?: throw IllegalStateException("Current round is not MEMORY")
        if (position < 0 || position >= sequence.colors.size ||
            sequence.colors[position] != color
        ) {
            registerMistake()
            memoryPosition = 0
            _state = _state.copy(round = round.copy(solvedInRow = 0))
            return AnswerOutcome.MISTAKE
        }
        memoryPosition += 1
        _state = _state.copy(round = round.copy(solvedInRow = memoryPosition))
        return if (memoryPosition >= sequence.colors.size) {
            completeBlock(TaskType.MEMORY)
            AnswerOutcome.BLOCK_DONE
        } else {
            AnswerOutcome.CORRECT
        }
    }

    /** Called by the UI when it restarts the flash after a mistake. */
    fun onMemoryReplay() {
        memoryPosition = 0
    }

    /** Convenience overload: taps at the engine's internal position. */
    fun onMemoryTap(color: TaskColor): AnswerOutcome = onMemoryTap(memoryPosition, color)

    // --- STROOP -------------------------------------------------------------

    /**
     * Submits the pressed color-name button for the current Stroop
     * round. The answer is correct when it matches the INK color.
     * A wrong answer resets the in-task streak with a new round.
     */
    fun onStroopSubmit(color: TaskColor): AnswerOutcome {
        val round = _state.round as? RoundState.Stroop
            ?: throw IllegalStateException("Current round is not STROOP")
        val correct = round.inkColor == color
        return if (correct) {
            stroopStreak += 1
            if (stroopStreak >= round.requiredInRow) {
                completeBlock(TaskType.STROOP)
                AnswerOutcome.BLOCK_DONE
            } else {
                nextStroopRound()
                AnswerOutcome.CORRECT
            }
        } else {
            registerMistake()
            stroopStreak = 0
            nextStroopRound()
            AnswerOutcome.MISTAKE
        }
    }

    // --- Internals ------------------------------------------------------------

    private fun startBlock(index: Int) {
        blockStartedAt = clock()
        blockMistakes = 0
        mathStreak = 0
        stroopStreak = 0
        memoryPosition = 0
        memorySequence = null
        val type = blockTypes[index]
        val round = when (type) {
            TaskType.MATH -> {
                currentMathData = mathGenerator.generate(difficulty)
                RoundState.Math(
                    expression = currentMathData.expression,
                    solvedInRow = 0,
                    requiredInRow = params.mathAnswersInRow
                )
            }
            TaskType.MEMORY -> RoundState.Memory(params.memorySequenceLength, solvedInRow = 0)
            TaskType.STROOP -> stroopRoundState()
        }
        _state = _state.copy(taskIndex = index, round = round)
    }

    private fun stroopRoundState(): RoundState.Stroop {
        val data = stroopGenerator.generate(difficulty)
        return RoundState.Stroop(
            wordColor = data.wordColor,
            inkColor = data.inkColor,
            solvedInRow = stroopStreak,
            requiredInRow = params.stroopRounds
        )
    }

    private fun nextMathRound() {
        currentMathData = mathGenerator.generate(difficulty)
        _state = _state.copy(
            round = RoundState.Math(
                expression = currentMathData.expression,
                solvedInRow = mathStreak,
                requiredInRow = params.mathAnswersInRow
            )
        )
    }

    private fun nextStroopRound() {
        _state = _state.copy(round = stroopRoundState())
    }

    /** Counts a mistake for the session AND the current block. */
    private fun registerMistake() {
        blockMistakes += 1
        _state = _state.copy(totalMistakes = _state.totalMistakes + 1)
    }

    private fun completeBlock(type: TaskType) {
        recordedResults += TaskResult(
            timeTakenMs = (clock() - blockStartedAt).coerceAtLeast(0L),
            mistakes = blockMistakes,
            taskType = type
        )
        val nextIndex = _state.taskIndex + 1
        if (nextIndex >= blockTypes.size) {
            _state = _state.copy(isFinished = true)
        } else {
            startBlock(nextIndex)
        }
    }
}
