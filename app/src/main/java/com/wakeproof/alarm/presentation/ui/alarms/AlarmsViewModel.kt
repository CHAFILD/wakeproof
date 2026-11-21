package com.wakeproof.alarm.presentation.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.repository.AlarmRepository
import com.wakeproof.alarm.domain.time.NextTriggerCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

/**
 * One row of the alarm list: the domain alarm plus the UI-only countdown
 * ("fires in Xh Ym") recalculated every minute.
 */
data class AlarmListItem(
    val alarm: Alarm,
    val minutesUntilTrigger: Long?
)

/**
 * UI state of the main (alarms) screen.
 */
data class AlarmsUiState(
    val alarms: List<AlarmListItem> = emptyList()
)

/**
 * ViewModel for the alarms list. Reloads on every Room change and refreshes
 * the countdown each minute while the screen is visible. Survives rotation
 * because [uiState] is a StateFlow held by the ViewModel.
 */
class AlarmsViewModel(
    private val alarmRepository: AlarmRepository,
    private val clock: Clock
) : ViewModel() {

    private val minuteTicker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_PERIOD_MS)
        }
    }

    val uiState: StateFlow<AlarmsUiState> =
        combine(alarmRepository.observeAlarms(), minuteTicker) { alarms, _ ->
            val now = LocalDateTime.now(clock)
            AlarmsUiState(
                alarms = alarms.map { alarm ->
                    AlarmListItem(
                        alarm = alarm,
                        minutesUntilTrigger = NextTriggerCalculator.minutesUntilTrigger(alarm, now)
                    )
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AlarmsUiState()
        )

    /** Enables/disables the alarm; the Room flow re-emits the new state. */
    fun onToggleEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.setEnabled(alarm.id, enabled)
        }
    }

    companion object {
        fun factory(repository: AlarmRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AlarmsViewModel(repository, Clock.systemDefaultZone()) as T
            }

        private const val TICK_PERIOD_MS = 60_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
