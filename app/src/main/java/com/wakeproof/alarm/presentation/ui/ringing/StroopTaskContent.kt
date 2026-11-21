package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.wakeproof.alarm.domain.task.TaskColor
import kotlinx.coroutines.delay

/**
 * Stroop task UI (spec 4.2.1): the color name printed in a DIFFERENT
 * ink color; the user must press the INK color button. The word is
 * huge and colored with the ink; the four answer buttons are neutral
 * (their labels must not hint the answer by their own color).
 */
@Composable
fun StroopTaskContent(
    round: RoundState.Stroop,
    onSubmit: (TaskColor) -> Boolean
) {
    // Brief red feedback after a wrong tap; survives the round change.
    var wrongColor by remember { mutableStateOf<TaskColor?>(null) }
    LaunchedEffect(wrongColor) {
        if (wrongColor != null) {
            delay(500L)
            wrongColor = null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(round.wordColor.labelRes),
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = round.inkColor.fill
        )
        Spacer(modifier = Modifier.height(18.dp))
        StreakDots(solved = round.solvedInRow, required = round.requiredInRow)
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.stroop_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TASK_GRID_COLORS.chunked(2).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowColors.forEach { color ->
                        val isWrong = wrongColor == color
                        OutlinedButton(
                            onClick = {
                                if (!onSubmit(color)) {
                                    wrongColor = color
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isWrong) {
                                BorderStroke(4.dp, MaterialTheme.colorScheme.error)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 76.dp)
                        ) {
                            Text(
                                text = stringResource(color.labelRes),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
