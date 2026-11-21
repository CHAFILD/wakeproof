package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeproof.alarm.R
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Morning summary, spec 4.1.3:
 *
 * ```
 * ☀️ Доброе утро!
 * ⏱ Задачи: 1:42 · ошибок: 1
 * 🔥 Серия: 12 утр          <- streak, later stage
 * ✅ Контрольный звонок…    <- recheck, later stage
 * [Закрыть]
 * ```
 *
 * The streak and recheck rows arrive in the next stages; their slots
 * are the marked spacer + row sections below so the layout is ready
 * to take them without restructuring.
 */
@Composable
fun SessionSummaryContent(
    summary: SummaryUiState?,
    onCloseUi: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.summary_good_morning),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(
                R.string.summary_tasks_line,
                formatSolveTime(summary?.solveTimeMs ?: 0L),
                summary?.mistakes ?: 0
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        // --- Streak slot (spec 4.4, later stage) -------------------
        Spacer(modifier = Modifier.height(8.dp))
        // --- Recheck slot (spec 4.3, later stage) ------------------
        Spacer(modifier = Modifier.height(8.dp))
        // -----------------------------------------------------------
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onCloseUi,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(80.dp)
        ) {
            Text(
                text = stringResource(R.string.summary_close),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** "m:ss" of the summed task solve time. */
private fun formatSolveTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
