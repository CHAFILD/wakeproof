package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeproof.alarm.R
import com.wakeproof.alarm.domain.session.RoundState
import kotlinx.coroutines.delay

/**
 * Math task UI (spec 4.2.1): one huge expression, a big on-screen
 * digit keypad (a half-asleep user must not fight the system
 * keyboard), the typed answer and OK. The streak dots show the
 * "N correct in a row" progress of the block.
 */
@Composable
fun MathTaskContent(
    round: RoundState.Math,
    onSubmit: (Int) -> Boolean
) {
    // New expression -> empty answer field (correct answer or mistake
    // both produce a fresh example in the engine). The wrong-flash
    // flag deliberately survives the expression change so the red
    // feedback is actually visible.
    var answer by remember(round.expression) { mutableStateOf("") }
    var isWrongFlash by remember { mutableStateOf(false) }
    LaunchedEffect(isWrongFlash) {
        if (isWrongFlash) {
            delay(500L)
            isWrongFlash = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = round.expression,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(14.dp))
        StreakDots(solved = round.solvedInRow, required = round.requiredInRow)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = answer.ifEmpty { stringResource(R.string.math_answer_placeholder) },
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isWrongFlash) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        MathKeypad(
            onDigit = { digit ->
                if (answer.length < MAX_ANSWER_DIGITS) answer += digit
            },
            onBackspace = { if (answer.isNotEmpty()) answer = answer.dropLast(1) },
            onOk = {
                val value = answer.toIntOrNull()
                if (value != null) {
                    isWrongFlash = !onSubmit(value)
                    answer = ""
                }
            }
        )
    }
}

/**
 * Big 3x4 keypad: digits 1-9, backspace, 0, OK. All buttons are at
 * least 64dp tall with 24sp+ text (dark room, sleepy fingers).
 */
@Composable
private fun MathKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onOk: () -> Unit
) {
    val buttonShape = MaterialTheme.shapes.large
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowKeys.forEach { key ->
                    KeypadKey(
                        text = key,
                        onClick = { onDigit(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = buttonShape
            ) {
                Text(
                    text = stringResource(R.string.keypad_backspace),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            KeypadKey(
                text = "0",
                onClick = { onDigit("0") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onOk,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.keypad_ok),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private const val MAX_ANSWER_DIGITS = 6
