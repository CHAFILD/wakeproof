package com.wakeproof.alarm.presentation.ui.ringing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeproof.alarm.R
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * The ringing screen (spec 4.1.3 / 4.1.4): full-screen, over the lock
 * screen, ONE task on screen at a time, no menus and no ads (spec
 * 1.4). The phases:
 *
 * - TASKS - the task queue from the alarm's settings with the progress
 *   header «Задача N из M» and the mistakes counter;
 * - SUMMARY - the morning summary after all tasks are solved;
 * - EMERGENCY_PHRASE / EMERGENCY_DONE - the hidden emergency exit.
 *
 * Back is swallowed; the service re-fires the screen if the user
 * leaves (stage 2). The alarm can ONLY be dismissed by solving the
 * tasks or through the emergency exit.
 */
@Composable
fun RingingScreen(
    viewModel: RingingViewModel,
    onCloseUi: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Spec 4.1.3: Back does not leave the ringing screen.
    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        when (uiState.phase) {
            RingingPhase.LOADING -> LoadingContent(uiState.alarmTimeLabel)

            RingingPhase.TASKS -> {
                TasksContent(
                    task = uiState.task,
                    alarmTimeLabel = uiState.alarmTimeLabel,
                    onMathSubmit = viewModel::onMathSubmit,
                    onMemorySequence = viewModel::currentMemorySequence,
                    onMemorySequenceShown = viewModel::onMemorySequenceShown,
                    onMemoryTap = viewModel::onMemoryTap,
                    onStroopSubmit = viewModel::onStroopSubmit
                )
                // Spec 4.1.4: hidden corner, 5-second hold.
                EmergencyCorner(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onActivated = viewModel::onEmergencyCornerActivated
                )
            }

            RingingPhase.SUMMARY -> SessionSummaryContent(
                summary = uiState.summary,
                onCloseUi = onCloseUi
            )

            RingingPhase.EMERGENCY_PHRASE -> EmergencyPhraseContent(
                hintPhrase = uiState.emergencyHintPhrase,
                isWrong = uiState.isEmergencyPhraseWrong,
                onPhraseChanged = viewModel::onEmergencyPhraseChanged,
                onSubmit = viewModel::onEmergencySubmit,
                onBackToTasks = viewModel::onEmergencyBackToTasks
            )

            RingingPhase.EMERGENCY_DONE -> EmergencyDoneContent(onCloseUi = onCloseUi)
        }
    }
}

/** Live clock; big while loading, compact above the task header. */
@Composable
private fun ClockText(fontSize: Int, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000L)
        }
    }
    Text(
        text = formatClock(now),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
private fun LoadingContent(alarmTimeLabel: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ClockText(fontSize = 88)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = alarmTimeLabel?.let { stringResource(R.string.ringing_alarm_at, it) }
                ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatClock(time: LocalTime): String =
    "%02d:%02d".format(Locale.ROOT, time.hour, time.minute)
