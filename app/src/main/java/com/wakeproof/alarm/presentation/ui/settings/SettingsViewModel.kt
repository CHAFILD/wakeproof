package com.wakeproof.alarm.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakeproof.alarm.domain.repository.EmergencyPhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** Null while the phrase is being loaded/generated on first use. */
    val emergencyPhrase: String? = null,
    /** True while the regenerate confirmation dialog is shown. */
    val isRegenerateDialogVisible: Boolean = false,
    /** True while a regeneration is in flight. */
    val isRegenerating: Boolean = false
)

/**
 * ViewModel of the Settings screen. Opening it also triggers the very
 * first phrase generation (spec 4.1.4: generated once at first use)
 * through getOrCreatePhrase().
 */
class SettingsViewModel(
    private val emergencyPhraseRepository: EmergencyPhraseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val phrase = emergencyPhraseRepository.getOrCreatePhrase()
            _uiState.update { it.copy(emergencyPhrase = phrase) }
        }
    }

    fun showRegenerateDialog() =
        _uiState.update { it.copy(isRegenerateDialogVisible = true) }

    fun hideRegenerateDialog() =
        _uiState.update { it.copy(isRegenerateDialogVisible = false) }

    fun regeneratePhrase() {
        if (_uiState.value.isRegenerating) return
        _uiState.update { it.copy(isRegenerating = true) }
        viewModelScope.launch {
            val phrase = emergencyPhraseRepository.regeneratePhrase()
            _uiState.update {
                it.copy(
                    emergencyPhrase = phrase,
                    isRegenerating = false,
                    isRegenerateDialogVisible = false
                )
            }
        }
    }

    companion object {
        fun factory(
            emergencyPhraseRepository: EmergencyPhraseRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(emergencyPhraseRepository) as T
            }
    }
}
