package com.wakeproof.alarm.presentation.ui.ringing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.Locale

/**
 * The ringing screen (spec 4.1.3, stage 2): dark, huge clock, the
 * alarm's ring time and one big temporary action. No menus, no ads —
 * one primary action per screen (spec 1.4). Back is swallowed; the
 * service re-fires the screen if the user leaves. The temporary
 * "Выключить" button is replaced by the task flow in stage 4.
 */
@Composable
fun RingingScreen(
    alarmTimeLabel: String?,
    onDismiss: () -> Unit
) {
    // Spec 4.1.3: Back does not leave the ringing screen.
    BackHandler(enabled = true) {}

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = formatClock(now),
            fontSize = 88.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = alarmTimeLabel?.let { label ->
                stringResource(R.string.ringing_alarm_at, label)
            } ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(96.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.ringing_dismiss),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatClock(time: LocalTime): String =
    "%02d:%02d".format(Locale.ROOT, time.hour, time.minute)
