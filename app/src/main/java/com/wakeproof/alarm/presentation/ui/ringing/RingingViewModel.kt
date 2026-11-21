package com.wakeproof.alarm.presentation.ui.ringing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the ringing screen (stage 2): the ringing alarm's own
 * ring time as its identity ("Будильник · 07:30") and the live clock.
 */
data class RingingUiState(
    val isLoaded: Boolean = false,
    /** "HH:mm" of the ringing alarm's configured time; null while loading. */
    val alarmTimeLabel: String? = null
)

/** One-shot events the activity reacts to. */
sealed interface RingingEvent {
    /** The alarm vanished from Room (deleted mid-ring): stop ringing. */
    data object AlarmGone : RingingEvent
}

/**
 * ViewModel behind [RingingActivity]. Loads the alarm by id from the
 * intent; the temporary "Выключить" action (stage 4 will replace it
 * with tasks) is handled by the activity through the service intent.
 */
class RingingViewModel(
    private val alarmId: Long,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RingingUiState())
    val uiState: StateFlow<RingingUiState> = _uiState.asStateFlow()

    private val events = Channel<RingingEvent>(Channel.BUFFERED)
    val eventsFlow: Flow<RingingEvent> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (alarmId <= 0L) {
                // Defensive: the FSI always carries the id; a manual launch
                // without one just shows the plain ringing screen.
                _uiState.update { it.copy(isLoaded = true) }
                return@launch
            }
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm == null) {
                events.send(RingingEvent.AlarmGone)
            } else {
                _uiState.update {
                    it.copy(
                        isLoaded = true,
                        alarmTimeLabel = formatTime(alarm.hour, alarm.minute)
                    )
                }
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        "%02d:%02d".format(java.util.Locale.ROOT, hour, minute)

    companion object {
        fun factory(alarmId: Long, repository: AlarmRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RingingViewModel(alarmId, repository) as T
            }
    }
}
