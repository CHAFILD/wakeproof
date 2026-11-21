package com.wakeproof.alarm.presentation.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeproof.alarm.R
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.presentation.common.weekdayShortLabelRes
import java.time.DayOfWeek

/**
 * Alarm editor (spec 4.1.1): time picker, weekday chips, task type
 * multi-select (min 1), difficulty, snooze policy, volume ramp and the
 * recheck ring with its interval. Delete is available for saved alarms.
 * Dark theme and large controls: the app targets half-asleep people.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(
    onBack: () -> Unit,
    viewModel: AlarmEditorViewModel = viewModel(
        factory = AlarmEditorViewModel.factory(
            (LocalContext.current.applicationContext as WakeProofApplication).appContainer.alarmRepository
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                AlarmEditorEvent.Finished -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isNew) R.string.editor_new_title else R.string.editor_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (!state.isLoaded) {
                // Reserve space while the existing alarm is being read from Room.
                Box(modifier = Modifier.fillMaxWidth().height(240.dp))
            } else {
                TimeSection(state = state, viewModel = viewModel)
                DaysSection(state = state, viewModel = viewModel)
                TaskTypesSection(state = state, viewModel = viewModel)
                DifficultySection(state = state, viewModel = viewModel)
                SnoozeSection(state = state, viewModel = viewModel)
                VolumeSection(state = state, viewModel = viewModel)
                RecheckSection(state = state, viewModel = viewModel)

                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.editor_save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (!state.isNew) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.editor_delete),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.editor_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text(stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    SectionTitle(R.string.editor_section_time)
    // Composed only after the alarm is loaded, so initial values are correct;
    // every user change is pushed into the ViewModel through the state reads.
    val timeState = rememberTimePickerState(
        initialHour = state.hour,
        initialMinute = state.minute,
        is24Hour = true
    )
    LaunchedEffect(timeState.hour, timeState.minute) {
        viewModel.onTimeChanged(timeState.hour, timeState.minute)
    }
    // Center the picker on the screen: by default it hugs the left edge.
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TimePicker(state = timeState)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    SectionTitle(R.string.editor_section_days)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.values().forEach { day ->
            FilterChip(
                selected = day in state.selectedDays,
                onClick = { viewModel.onToggleDay(day) },
                label = {
                    Text(
                        text = stringResource(weekdayShortLabelRes(day)),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskTypesSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    SectionTitle(R.string.editor_section_tasks)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskType.entries.forEach { type ->
            FilterChip(
                selected = type in state.taskTypes,
                onClick = { viewModel.onToggleTaskType(type) },
                label = {
                    Text(
                        text = stringResource(taskTypeLabelRes(type)),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
    if (state.hasTaskTypesError) {
        Text(
            text = stringResource(R.string.editor_error_no_tasks),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifficultySection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    SectionTitle(R.string.editor_section_difficulty)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Difficulty.entries.forEach { difficulty ->
            FilterChip(
                selected = state.difficulty == difficulty,
                onClick = { viewModel.onDifficultySelected(difficulty) },
                label = {
                    Text(
                        text = stringResource(difficultyLabelRes(difficulty)),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SnoozeSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    val isHardcore = state.difficulty == Difficulty.HARDCORE
    SectionTitle(R.string.editor_section_snooze)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SnoozePolicy.entries.forEach { policy ->
            FilterChip(
                selected = state.snoozePolicy == policy,
                onClick = { viewModel.onSnoozePolicySelected(policy) },
                enabled = !isHardcore,
                label = {
                    Text(
                        text = stringResource(snoozePolicyLabelRes(policy)),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
    if (isHardcore) {
        EditorHint(R.string.editor_hardcore_snooze_hint)
    }
}

@Composable
private fun VolumeSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    val isHardcore = state.difficulty == Difficulty.HARDCORE
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.editor_section_volume),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = state.isVolumeRampEnabled,
            onCheckedChange = viewModel::onVolumeRampChanged,
            enabled = !isHardcore
        )
    }
    if (isHardcore) {
        EditorHint(R.string.editor_hardcore_volume_hint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecheckSection(
    state: AlarmEditorUiState,
    viewModel: AlarmEditorViewModel
) {
    val isHardcore = state.difficulty == Difficulty.HARDCORE
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.editor_section_recheck),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.editor_recheck_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = state.isRecheckEnabled,
            onCheckedChange = viewModel::onRecheckChanged,
            // Spec 4.2.1: in HARDCORE the recheck ring can not be
            // disabled (only its interval may change).
            enabled = !isHardcore
        )
    }
    if (isHardcore) {
        EditorHint(R.string.editor_hardcore_recheck_hint)
    }
    if (state.isRecheckEnabled) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Alarm.RECHECK_DELAY_ALLOWED_MIN.sorted().forEach { delayMin ->
                FilterChip(
                    selected = state.recheckDelayMin == delayMin,
                    onClick = { viewModel.onRecheckDelaySelected(delayMin) },
                    label = {
                        Text(
                            text = stringResource(R.string.recheck_interval_min, delayMin),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/** Small gray hint under a locked-in-hardcore section. */
@Composable
private fun EditorHint(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun taskTypeLabelRes(type: TaskType): Int = when (type) {
    TaskType.MATH -> R.string.task_type_math
    TaskType.MEMORY -> R.string.task_type_memory
    TaskType.STROOP -> R.string.task_type_stroop
}

private fun difficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARDCORE -> R.string.difficulty_hardcore
}

private fun snoozePolicyLabelRes(policy: SnoozePolicy): Int = when (policy) {
    SnoozePolicy.FORBIDDEN -> R.string.snooze_forbidden
    SnoozePolicy.ONCE -> R.string.snooze_once
    SnoozePolicy.ONCE_HARDER -> R.string.snooze_once_harder
}
