package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.wakeproof.alarm.R
import kotlinx.coroutines.delay

/**
 * The emergency exit zone (spec 4.1.4): the top-right corner of the
 * ringing screen, held for 5 seconds. A small semi-transparent warning
 * triangle makes the zone discoverable (product decision: a fully
 * invisible corner was too hard to hit); holding brightens the icon
 * and runs a thin progress line under it. Releasing early cancels.
 */
@Composable
fun EmergencyCorner(
    modifier: Modifier = Modifier,
    onActivated: () -> Unit
) {
    var isHeld by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isHeld) {
        if (isHeld) {
            while (progress < 1f) {
                delay(HOLD_STEP_MS)
                progress = (progress + HOLD_STEP_MS / HOLD_DURATION_MS).coerceAtMost(1f)
            }
            isHeld = false
            progress = 0f
            onActivated()
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .size(CORNER_SIZE_DP.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHeld = true
                        tryAwaitRelease()
                        isHeld = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = stringResource(R.string.cd_emergency_corner),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (isHeld) 1f else IDLE_ICON_ALPHA
            ),
            modifier = Modifier.size(30.dp)
        )
        // Thin progress line at the bottom edge while holding.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width((CORNER_SIZE_DP - 8).dp * progress)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Emergency exit screen (spec 4.1.4): input field + THE PHRASE itself
 * as a small gray hint below (uncomfortable to type while half-asleep,
 * but never a dead end). Wrong input just shows an error; the user can
 * always go back to the tasks.
 */
@Composable
fun EmergencyPhraseContent(
    hintPhrase: String?,
    isWrong: Boolean,
    onPhraseChanged: () -> Unit,
    onSubmit: (String) -> Unit,
    onBackToTasks: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.emergency_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                onPhraseChanged()
            },
            singleLine = true,
            label = { Text(stringResource(R.string.emergency_input_hint)) },
            isError = isWrong,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        if (isWrong) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.emergency_wrong),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // The phrase itself as a small gray hint, spec 4.1.4.
        Text(
            text = hintPhrase.orEmpty(),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(36.dp))
        Button(
            onClick = { onSubmit(input) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(
                text = stringResource(R.string.emergency_button),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackToTasks) {
            Text(
                text = stringResource(R.string.emergency_back),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Final emergency screen: ringing already stopped, one close action. */
@Composable
fun EmergencyDoneContent(onCloseUi: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.emergency_done_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.emergency_done_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onCloseUi,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(80.dp)
        ) {
            Text(
                text = stringResource(R.string.summary_close),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private const val HOLD_DURATION_MS = 5_000L
private const val HOLD_STEP_MS = 50L

/** Touch zone of the emergency corner (spec 4.1.4: small area). */
private const val CORNER_SIZE_DP = 64

/** Idle visibility of the warning triangle: noticeable, not shouting. */
private const val IDLE_ICON_ALPHA = 0.45f
