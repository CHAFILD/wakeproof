package com.wakeproof.alarm.presentation.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.repository.AlarmRepository
import com.wakeproof.alarm.presentation.navigation.Destinations
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/**
 * One-shot events the screen reacts to with navigation.
 */
sealed interface AlarmEditorEvent {
    /** Editor is done (saved or deleted) — go back to the list. */
    data object Finished : AlarmEditorEvent
}

/**
 * Full editor state. State lives in the ViewModel, so it survives
 * configuration changes.
 */
data class AlarmEditorUiState(
    val isNew: Boolean = true,
    /** False until the existing alarm (if any) is loaded from Room. */
    val isLoaded: Boolean = false,
    val hour: Int = Alarm.DEFAULT_HOUR,
    val minute: Int = Alarm.DEFAULT_MINUTE,
    /** Empty set = every day. */
    val selectedDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val taskTypes: Set<TaskType> = setOf(TaskType.MATH, TaskType.STROOP),
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val snoozePolicy: SnoozePolicy = SnoozePolicy.FORBIDDEN,
    val isVolumeRampEnabled: Boolean = true,
    val isRecheckEnabled: Boolean = true,
    val recheckDelayMin: Int = Alarm.RECHECK_DELAY_DEFAULT_MIN,
    /** Set when the user tried to save with no task types selected. */
    val hasTaskTypesError: Boolean = false
)

/**
 * ViewModel of the alarm editor (create/edit/delete, spec 4.1.1).
 * The alarm id comes from navigation through [SavedStateHandle];
 * [Destinations.NEW_ALARM_ID] means a new alarm.
 */
class AlarmEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val alarmId: Long =
        savedStateHandle[Destinations.ARG_ALARM_ID] ?: Destinations.NEW_ALARM_ID

    /**
     * Fields that have no editor UI yet (task count, QR/NFC anchor, sleep
     * reminder) are copied from the loaded alarm on save, so editing never
     * silently resets them.
     */
    private var loadedAlarm: Alarm? = null

    private val _uiState = MutableStateFlow(
        AlarmEditorUiState(isNew = alarmId == Destinations.NEW_ALARM_ID)
    )
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    private val events = Channel<AlarmEditorEvent>(Channel.BUFFERED)
    val eventsFlow: Flow<AlarmEditorEvent> = events.receiveAsFlow()

    init {
        if (alarmId == Destinations.NEW_ALARM_ID) {
            _uiState.update { it.copy(isLoaded = true) }
        } else {
            viewModelScope.launch {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm == null) {
                    // Nothing to edit (already deleted) — close the editor.
                    events.send(AlarmEditorEvent.Finished)
                } else {
                    loadedAlarm = alarm
                    _uiState.update {
                        it.copy(
                            isLoaded = true,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            selectedDays = alarm.daysOfWeek,
                            taskTypes = alarm.taskTypes,
                            difficulty = alarm.difficulty,
                            snoozePolicy = alarm.snoozePolicy,
                            isVolumeRampEnabled = alarm.isVolumeRampEnabled,
                            isRecheckEnabled = alarm.isRecheckEnabled,
                            recheckDelayMin = alarm.recheckDelayMin
                        )
                    }
                }
            }
        }
    }

    fun onTimeChanged(hour: Int, minute: Int) =
        _uiState.update { it.copy(hour = hour, minute = minute) }

    fun onToggleDay(day: DayOfWeek) = _uiState.update {
        it.copy(selectedDays = if (day in it.selectedDays) it.selectedDays - day else it.selectedDays + day)
    }

    /** Spec: at least one task type must stay selected (multi-select). */
    fun onToggleTaskType(type: TaskType) = _uiState.update {
        if (type in it.taskTypes) {
            val remaining = it.taskTypes - type
            if (remaining.isEmpty()) {
                it.copy(hasTaskTypesError = true)
            } else {
                it.copy(taskTypes = remaining, hasTaskTypesError = false)
            }
        } else {
            it.copy(taskTypes = it.taskTypes + type, hasTaskTypesError = false)
        }
    }

    fun onDifficultySelected(difficulty: Difficulty) =
        _uiState.update { it.copy(difficulty = difficulty) }

    fun onSnoozePolicySelected(policy: SnoozePolicy) =
        _uiState.update { it.copy(snoozePolicy = policy) }

    fun onVolumeRampChanged(enabled: Boolean) =
        _uiState.update { it.copy(isVolumeRampEnabled = enabled) }

    fun onRecheckChanged(enabled: Boolean) =
        _uiState.update { it.copy(isRecheckEnabled = enabled) }

    fun onRecheckDelaySelected(delayMin: Int) =
        _uiState.update { it.copy(recheckDelayMin = delayMin) }

    /** Validates and persists the alarm, then finishes the editor. */
    fun save() {
        val state = _uiState.value
        if (state.taskTypes.isEmpty()) {
            // Defensive: the UI already prevents emptying the selection.
            _uiState.update { it.copy(hasTaskTypesError = true) }
            return
        }
        viewModelScope.launch {
            val base = loadedAlarm ?: Alarm()
            val alarm = base.copy(
                id = if (state.isNew) 0 else alarmId,
                hour = state.hour,
                minute = state.minute,
                daysOfWeek = state.selectedDays,
                taskTypes = state.taskTypes,
                difficulty = state.difficulty,
                snoozePolicy = state.snoozePolicy,
                isVolumeRampEnabled = state.isVolumeRampEnabled,
                isRecheckEnabled = state.isRecheckEnabled,
                recheckDelayMin = state.recheckDelayMin
            )
            alarmRepository.upsert(alarm)
            events.send(AlarmEditorEvent.Finished)
        }
    }

    fun delete() {
        if (alarmId == Destinations.NEW_ALARM_ID) {
            events.trySend(AlarmEditorEvent.Finished)
            return
        }
        viewModelScope.launch {
            alarmRepository.deleteAlarmById(alarmId)
            events.send(AlarmEditorEvent.Finished)
        }
    }

    companion object {
        fun factory(repository: AlarmRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    AlarmEditorViewModel(extras.createSavedStateHandle(), repository) as T
            }
    }
}
