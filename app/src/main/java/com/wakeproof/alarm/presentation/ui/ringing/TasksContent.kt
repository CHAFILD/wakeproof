package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeproof.alarm.R
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.session.RoundState
import com.wakeproof.alarm.domain.task.TaskColor

/**
 * Task part of the ringing screen (spec 4.1.3): compact clock, the
 * progress header («Задача 1 из 2» + mistakes) and exactly ONE task
 * block at a time. Big fonts and buttons: the user is half-asleep in
 * the dark.
 */
@Composable
fun TasksContent(
    task: TaskUiState?,
    alarmTimeLabel: String?,
    onMathSubmit: (Int) -> Boolean,
    onMemorySequence: () -> List<TaskColor>,
    onMemorySequenceShown: () -> Unit,
    onMemoryTap: (TaskColor) -> Boolean,
    onStroopSubmit: (TaskColor) -> Boolean
) {
    if (task == null) return
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TaskHeader(
            alarmTimeLabel = alarmTimeLabel,
            taskNumber = task.taskNumber,
            totalTasks = task.totalTasks,
            mistakes = task.totalMistakes,
            taskType = task.round.typeOfTask
        )
        Spacer(modifier = Modifier.height(20.dp))
        when (val round = task.round) {
            is RoundState.Math -> MathTaskContent(
                round = round,
                onSubmit = onMathSubmit
            )
            is RoundState.Memory -> MemoryTaskContent(
                round = round,
                memoryPhase = task.memoryPhase,
                memoryShowId = task.memoryShowId,
                loadSequence = onMemorySequence,
                onSequenceShown = onMemorySequenceShown,
                onTap = onMemoryTap
            )
            is RoundState.Stroop -> StroopTaskContent(
                round = round,
                onSubmit = onStroopSubmit
            )
        }
    }
}

@Composable
private fun TaskHeader(
    alarmTimeLabel: String?,
    taskNumber: Int,
    totalTasks: Int,
    mistakes: Int,
    taskType: TaskType
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = alarmTimeLabel ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.ringing_task_progress, taskNumber, totalTasks),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = pluralStringResource(
                    R.plurals.ringing_mistakes, mistakes, mistakes
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (mistakes == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(taskType.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Round type label for the header («Математика» / «Память» / «Stroop»). */
private val RoundState.typeOfTask: TaskType
    get() = when (this) {
        is RoundState.Math -> TaskType.MATH
        is RoundState.Memory -> TaskType.MEMORY
        is RoundState.Stroop -> TaskType.STROOP
    }

private val TaskType.labelRes: Int
    get() = when (this) {
        TaskType.MATH -> R.string.task_type_math
        TaskType.MEMORY -> R.string.task_type_memory
        TaskType.STROOP -> R.string.task_type_stroop
    }

/** Big streak dots: filled = solved in a row, outline = remaining. */
@Composable
fun StreakDots(solved: Int, required: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(required) { index ->
            val filled = index < solved
            Text(
                text = stringResource(
                    if (filled) R.string.streak_dot_filled else R.string.streak_dot_empty
                ),
                fontSize = 20.sp,
                color = if (filled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
