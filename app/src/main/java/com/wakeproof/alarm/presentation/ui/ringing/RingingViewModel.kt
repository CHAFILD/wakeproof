package com.wakeproof.alarm.presentation.ui.ringing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.emergency.EmergencyPhraseMatcher
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.repository.AlarmRepository
import com.wakeproof.alarm.domain.repository.EmergencyPhraseRepository
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.session.AnswerOutcome
import com.wakeproof.alarm.domain.session.RoundState
import com.wakeproof.alarm.domain.session.SessionParamsResolver
import com.wakeproof.alarm.domain.session.TaskSessionEngine
import com.wakeproof.alarm.domain.session.TaskSessionRecorder
import com.wakeproof.alarm.domain.snooze.SnoozeMachine
import com.wakeproof.alarm.domain.streak.StreakCalculator
import com.wakeproof.alarm.domain.task.TaskColor
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Screen-level phases of the ringing flow. */
enum class RingingPhase {
    /** Alarm is being loaded from Room. */
    LOADING,

    /** Solving the task queue, spec 4.1.3. */
    TASKS,

    /** All tasks solved - morning summary, spec 4.1.3. */
    SUMMARY,

    /** Emergency exit phrase input, spec 4.1.4. */
    EMERGENCY_PHRASE,

    /** Emergency exit completed; only the close button remains. */
    EMERGENCY_DONE
}

/** UI phases of the memory round: flash animation vs user input. */
enum class MemoryPhase { SHOWING, INPUT }

/** Task-part of the UI state; mirrors the engine snapshot. */
data class TaskUiState(
    /** 1-based task number for «Задача N из M». */
    val taskNumber: Int,
    val totalTasks: Int,
    val totalMistakes: Int,
    val round: RoundState,
    val memoryPhase: MemoryPhase,
    /** Increments on every flash start; drives the replay animation. */
    val memoryShowId: Int
)

/** Morning summary values, spec 4.1.3. */
data class SummaryUiState(
    /** Sum of the solved tasks' durations. */
    val solveTimeMs: Long,
    val mistakes: Int,
    /** Streak including the morning that just finished (spec 4.4). */
    val streakMornings: Int
)

data class RingingUiState(
    val isLoaded: Boolean = false,
    /** "HH:mm" of the ringing alarm's configured time; null while loading. */
    val alarmTimeLabel: String? = null,
    val phase: RingingPhase = RingingPhase.LOADING,
    val task: TaskUiState? = null,
    val summary: SummaryUiState? = null,
    /** True when the ring screen may show the snooze button (spec 4.1.2). */
    val isSnoozeAvailable: Boolean = false,
    /** The phrase shown as a small gray hint on the emergency screen. */
    val emergencyHintPhrase: String? = null,
    val isEmergencyPhraseWrong: Boolean = false
)

/** One-shot events the activity reacts to. */
sealed interface RingingEvent {
    /** The alarm vanished from Room (deleted mid-ring): stop ringing. */
    data object AlarmGone : RingingEvent

    /**
     * The alarm is solved/emergency-exited: stop the sound and the
     * service and re-arm the schedule, but KEEP the UI on screen
     * (summary / emergency-done screens).
     */
    data object StopRinging : RingingEvent

    /**
     * Snooze pressed (spec 4.1.2): the session is stored as SNOOZED,
     * the ring is re-armed for +10 minutes; the ringing UI closes.
     */
    data object SnoozeArmed : RingingEvent
}

/**
 * ViewModel behind [RingingActivity]. Builds the task queue from the
 * alarm's settings via the stage-3 engine, records the session into
 * Room and drives the emergency exit flow (spec 4.1.3 / 4.1.4).
 */
class RingingViewModel(
    private val alarmId: Long,
    private val alarmRepository: AlarmRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val emergencyPhraseRepository: EmergencyPhraseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RingingUiState())
    val uiState: StateFlow<RingingUiState> = _uiState.asStateFlow()

    private val events = Channel<RingingEvent>(Channel.BUFFERED)
    val eventsFlow: Flow<RingingEvent> = events.receiveAsFlow()

    private var engine: TaskSessionEngine? = null
    private var recorder: TaskSessionRecorder? = null

    /** The alarm this ring belongs to; needed by the snooze decision. */
    private var loadedAlarm: Alarm? = null

    /** Guards a double tap on the snooze button. */
    private var isSnoozeInFlight = false

    init {
        viewModelScope.launch {
            if (alarmId <= 0L) {
                // Defensive: the FSI always carries the id; a manual
                // launch without one has nothing to solve.
                events.send(RingingEvent.AlarmGone)
                return@launch
            }
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm == null) {
                events.send(RingingEvent.AlarmGone)
            } else {
                startSession(alarm)
            }
        }
    }

    private fun startSession(alarm: Alarm) {
        loadedAlarm = alarm
        // Spec 4.2.1: HARDCORE always rings with 4 tasks; an armed
        // snoozed ring uses the parameters fixed at snooze time (spec
        // 4.1.2: same params for ONCE, +1 task / one level up for
        // ONCE_HARDER).
        val sessionParams = SessionParamsResolver.resolve(alarm)
        // A snoozed ring continues the SAME morning session row (spec
        // 1.5: session = from firing to dismissal; a snoozed morning
        // stays SNOOZED even when the repeated ring is solved).
        val isSnoozeRing = alarm.snoozeSessionId > 0L
        engine = TaskSessionEngine(
            selectedTypes = alarm.taskTypes,
            taskCount = sessionParams.taskCount,
            difficulty = sessionParams.difficulty
        )
        recorder = TaskSessionRecorder(
            alarmId = alarmId,
            repository = taskSessionRepository,
            continuationOf = if (isSnoozeRing) alarm.snoozeSessionId else 0L
        )
        _uiState.update {
            it.copy(
                isLoaded = true,
                alarmTimeLabel = formatTime(alarm.hour, alarm.minute),
                phase = RingingPhase.TASKS,
                // Spec 4.1.2: the snooze button appears only when the
                // policy allows it and the once-per-firing limit is
                // intact; HARDCORE never shows it.
                isSnoozeAvailable = SnoozeMachine.canSnooze(alarm)
            )
        }
        publishTasksState()
    }

    // --- Snooze (spec 4.1.2) ----------------------------------------------------

    /**
     * The +10 minutes button: stores the morning session as SNOOZED,
     * fixes the snoozed-session parameters and the 10-minute target in
     * Room (reboot/process-death proof), then stops the ringing - the
     * service's dismiss re-arm schedules the snooze target because the
     * alarm row now carries it.
     */
    fun onSnoozePressed() {
        val alarm = loadedAlarm ?: return
        if (_uiState.value.phase != RingingPhase.TASKS) return
        if (!SnoozeMachine.canSnooze(alarm) || isSnoozeInFlight) return
        isSnoozeInFlight = true
        val eng = engine
        viewModelScope.launch {
            val finished = recorder?.finish(
                result = TaskSessionResult.SNOOZED,
                taskResults = eng?.results ?: emptyList(),
                totalMistakes = eng?.state?.totalMistakes ?: 0
            )
            val decision = SnoozeMachine.decideSnooze(alarm, System.currentTimeMillis())
            val armed = alarm.copy(
                snoozeTargetAt = decision.targetAtMillis,
                isSnoozeUsed = true,
                snoozeSessionTaskCount = decision.sessionTaskCount,
                snoozeSessionDifficulty = decision.sessionDifficulty,
                snoozeSessionId = finished?.id ?: alarm.snoozeSessionId
            )
            alarmRepository.updateSnoozeState(armed)
            events.send(RingingEvent.SnoozeArmed)
        }
    }

    // --- Math round ------------------------------------------------------------

    /** @return true when the submitted answer was correct (UI feedback). */
    fun onMathSubmit(answer: Int): Boolean {
        val eng = engine ?: return false
        if (eng.state.isFinished || _uiState.value.phase != RingingPhase.TASKS) return false
        return when (eng.onMathSubmit(answer)) {
            AnswerOutcome.BLOCK_DONE -> {
                maybeFinishSession()
                true
            }
            AnswerOutcome.CORRECT -> {
                publishTasksState()
                true
            }
            AnswerOutcome.MISTAKE -> {
                publishTasksState()
                false
            }
        }
    }

    // --- Memory round ------------------------------------------------------------

    /** The sequence to flash; the engine keeps it stable across replays. */
    fun currentMemorySequence(): List<TaskColor> =
        engine?.memorySequenceColors() ?: emptyList()

    /** Called by the UI when the flash animation finished. */
    fun onMemorySequenceShown() {
        _uiState.update { state ->
            state.copy(
                task = state.task?.copy(memoryPhase = MemoryPhase.INPUT)
            )
        }
    }

    /** @return true when the tapped color matched the expected position. */
    fun onMemoryTap(color: TaskColor): Boolean {
        val eng = engine ?: return false
        if (eng.state.isFinished || _uiState.value.phase != RingingPhase.TASKS) return false
        return when (eng.onMemoryTap(color)) {
            AnswerOutcome.MISTAKE -> {
                // Same sequence replays from the start (no dead ends).
                publishTasksState(forceShow = true)
                false
            }
            AnswerOutcome.CORRECT -> {
                publishTasksState()
                true
            }
            AnswerOutcome.BLOCK_DONE -> {
                maybeFinishSession()
                true
            }
        }
    }

    // --- Stroop round ------------------------------------------------------------

    /** @return true when the pressed button matched the INK color. */
    fun onStroopSubmit(color: TaskColor): Boolean {
        val eng = engine ?: return false
        if (eng.state.isFinished || _uiState.value.phase != RingingPhase.TASKS) return false
        return when (eng.onStroopSubmit(color)) {
            AnswerOutcome.BLOCK_DONE -> {
                maybeFinishSession()
                true
            }
            AnswerOutcome.CORRECT -> {
                publishTasksState()
                true
            }
            AnswerOutcome.MISTAKE -> {
                publishTasksState()
                false
            }
        }
    }

    // --- Emergency exit (spec 4.1.4) --------------------------------------------

    /** The hidden corner has been held for 5 seconds. */
    fun onEmergencyCornerActivated() {
        if (_uiState.value.phase != RingingPhase.TASKS) return
        viewModelScope.launch {
            val phrase = emergencyPhraseRepository.getOrCreatePhrase()
            _uiState.update {
                it.copy(
                    phase = RingingPhase.EMERGENCY_PHRASE,
                    emergencyHintPhrase = phrase,
                    isEmergencyPhraseWrong = false
                )
            }
        }
    }

    /** Clears the wrong-phrase error while the user keeps typing. */
    fun onEmergencyPhraseChanged() {
        if (_uiState.value.isEmergencyPhraseWrong) {
            _uiState.update { it.copy(isEmergencyPhraseWrong = false) }
        }
    }

    fun onEmergencySubmit(input: String) {
        val phrase = _uiState.value.emergencyHintPhrase ?: return
        if (_uiState.value.phase != RingingPhase.EMERGENCY_PHRASE) return
        if (!EmergencyPhraseMatcher.matches(input, phrase)) {
            _uiState.update { it.copy(isEmergencyPhraseWrong = true) }
            return
        }
        val eng = engine
        viewModelScope.launch {
            recorder?.finish(
                result = TaskSessionResult.EMERGENCY,
                taskResults = eng?.results ?: emptyList(),
                totalMistakes = eng?.state?.totalMistakes ?: 0
            )
            _uiState.update {
                it.copy(phase = RingingPhase.EMERGENCY_DONE, isEmergencyPhraseWrong = false)
            }
            events.send(RingingEvent.StopRinging)
        }
    }

    /** Back from the phrase screen to the tasks (no dead ends). */
    fun onEmergencyBackToTasks() {
        if (_uiState.value.phase != RingingPhase.EMERGENCY_PHRASE) return
        _uiState.update { it.copy(phase = RingingPhase.TASKS, isEmergencyPhraseWrong = false) }
    }

    // --- Internals ------------------------------------------------------------

    /**
     * Refreshes the task state after every answer. [forceShow] restarts
     * the memory flash (new memory block or replay after a mistake).
     */
    private fun publishTasksState(forceShow: Boolean = false) {
        val eng = engine ?: return
        val state = eng.state
        val previous = _uiState.value.task
        val round = state.round
        val isNewMemoryBlock = round is RoundState.Memory &&
            (previous == null ||
                previous.round !is RoundState.Memory ||
                previous.taskNumber != state.taskIndex + 1)
        val showFromScratch = round is RoundState.Memory && (forceShow || isNewMemoryBlock)
        val memoryPhase = when {
            round !is RoundState.Memory -> MemoryPhase.INPUT
            showFromScratch -> MemoryPhase.SHOWING
            else -> previous?.memoryPhase ?: MemoryPhase.SHOWING
        }
        val memoryShowId = when {
            round !is RoundState.Memory -> previous?.memoryShowId ?: 0
            showFromScratch -> (previous?.memoryShowId ?: 0) + 1
            else -> previous?.memoryShowId ?: 0
        }
        _uiState.update {
            it.copy(
                task = TaskUiState(
                    taskNumber = state.taskIndex + 1,
                    totalTasks = state.totalTasks,
                    totalMistakes = state.totalMistakes,
                    round = round,
                    memoryPhase = memoryPhase,
                    memoryShowId = memoryShowId
                )
            )
        }
    }

    private fun maybeFinishSession() {
        val eng = engine ?: return
        if (!eng.state.isFinished) {
            publishTasksState()
            return
        }
        val summary = SummaryUiState(
            solveTimeMs = eng.results.sumOf { it.timeTakenMs },
            mistakes = eng.state.totalMistakes,
            streakMornings = 0
        )
        viewModelScope.launch {
            recorder?.finish(
                result = TaskSessionResult.CLEAN,
                taskResults = eng.results,
                totalMistakes = eng.state.totalMistakes
            )
            // Spec 4.4: the summary shows the streak INCLUDING the
            // morning that just finished. A snoozed morning merged into
            // its SNOOZED row never counts as clean.
            val streak = StreakCalculator.currentStreak(
                taskSessionRepository.getAllChronological()
            )
            _uiState.update {
                it.copy(
                    phase = RingingPhase.SUMMARY,
                    summary = summary.copy(streakMornings = streak),
                    task = null
                )
            }
            events.send(RingingEvent.StopRinging)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        "%02d:%02d".format(Locale.ROOT, hour, minute)

    companion object {
        fun factory(
            alarmId: Long,
            alarmRepository: AlarmRepository,
            taskSessionRepository: TaskSessionRepository,
            emergencyPhraseRepository: EmergencyPhraseRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RingingViewModel(
                        alarmId,
                        alarmRepository,
                        taskSessionRepository,
                        emergencyPhraseRepository
                    ) as T
            }
    }
}
