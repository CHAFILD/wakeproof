package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeproof.alarm.R
import com.wakeproof.alarm.domain.session.RoundState
import com.wakeproof.alarm.domain.task.TaskColor
import kotlinx.coroutines.delay

/**
 * Memory task UI (spec 4.2.1): a 2x2 grid of big colored buttons. The
 * sequence flashes one button at a time (SHOWING), then the user
 * repeats it (INPUT). A wrong tap replays the SAME sequence from the
 * start - the user can always learn it, no dead ends (spec 1.4).
 */
@Composable
fun MemoryTaskContent(
    round: RoundState.Memory,
    memoryPhase: MemoryPhase,
    memoryShowId: Int,
    loadSequence: () -> List<TaskColor>,
    onSequenceShown: () -> Unit,
    onTap: (TaskColor) -> Boolean
) {
    // The color currently lit by the flash animation; null = none.
    var flashColor by remember { mutableStateOf<TaskColor?>(null) }
    // Brief red feedback after a wrong tap.
    var wrongColor by remember { mutableStateOf<TaskColor?>(null) }
    LaunchedEffect(wrongColor) {
        if (wrongColor != null) {
            delay(500L)
            wrongColor = null
        }
    }

    // Flash animation: runs on every new memoryShowId (new block or
    // replay after a mistake).
    LaunchedEffect(memoryShowId) {
        if (memoryPhase != MemoryPhase.SHOWING) return@LaunchedEffect
        flashColor = null
        delay(INITIAL_DELAY_MS)
        loadSequence().forEach { color ->
            flashColor = color
            delay(FLASH_ON_MS)
            flashColor = null
            delay(FLASH_GAP_MS)
        }
        onSequenceShown()
    }

    val isShowing = memoryPhase == MemoryPhase.SHOWING
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(
                if (isShowing) R.string.memory_watch_hint else R.string.memory_repeat_hint
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(14.dp))
        StreakDots(solved = round.solvedInRow, required = round.sequenceLength)
        Spacer(modifier = Modifier.height(14.dp))
        ColorGrid(
            isShowing = isShowing,
            flashColor = flashColor,
            wrongColor = wrongColor
        ) { color ->
            if (!isShowing) {
                if (!onTap(color)) {
                    wrongColor = color
                }
            }
        }
    }
}

/** The fixed 2x2 grid of the four task colors. */
@Composable
private fun ColorGrid(
    isShowing: Boolean,
    flashColor: TaskColor?,
    wrongColor: TaskColor?,
    onTap: (TaskColor) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TASK_GRID_COLORS.chunked(2).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowColors.forEach { color ->
                    ColorButton(
                        color = color,
                        isLit = flashColor == color,
                        isWrong = wrongColor == color,
                        enabled = !isShowing,
                        onClick = { onTap(color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: TaskColor,
    isLit: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isLit) 1.07f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "colorButtonScale"
    )
    val label = stringResource(color.labelRes)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .size(width = 132.dp, height = 110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = label },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLit) color.flashFill else color.fill,
            contentColor = color.fill
        ),
        border = when {
            isWrong -> BorderStroke(4.dp, MaterialTheme.colorScheme.error)
            isLit -> BorderStroke(4.dp, androidx.compose.ui.graphics.Color.White)
            else -> null
        }
    ) {
        // Pure color button; the color name lives in the content
        // description for TalkBack.
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
    }
}

private val FLASH_ON_MS = 550L
private val FLASH_GAP_MS = 250L
private val INITIAL_DELAY_MS = 400L
