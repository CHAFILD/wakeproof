package com.wakeproof.alarm.presentation.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.streak.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Streak values of the stats screen (spec 4.4 / 4.10). */
data class StatsUiState(
    /** Consecutive CLEAN mornings counted backwards from the newest. */
    val currentStreak: Int = 0,
    /** The longest CLEAN run in the whole history. */
    val recordStreak: Int = 0
)

/**
 * ViewModel of the stats screen: current and record streak over the
 * persisted ringing-session history (spec 4.4). The wakefulness score
 * chart and CSV export arrive in later stages and will extend this VM.
 */
class StatsViewModel(
    taskSessionRepository: TaskSessionRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> =
        taskSessionRepository.observeSessions().map { sessions ->
            StatsUiState(
                currentStreak = StreakCalculator.currentStreak(sessions),
                recordStreak = StreakCalculator.recordStreak(sessions)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = StatsUiState()
        )

    companion object {
        fun factory(repository: TaskSessionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StatsViewModel(repository) as T
            }

        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
