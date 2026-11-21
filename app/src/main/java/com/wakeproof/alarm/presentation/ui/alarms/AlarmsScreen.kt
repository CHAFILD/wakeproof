package com.wakeproof.alarm.presentation.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeproof.alarm.R
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.presentation.common.weekdayShortLabelRes
import com.wakeproof.alarm.presentation.navigation.Destinations
import java.time.DayOfWeek
import java.util.Locale

/**
 * Main screen: the alarm list with big time, weekday badges, countdown
 * ("fires in Xh Ym") and an on/off switch. The streak (spec 4.4) shows
 * under the nearest enabled alarm. Empty state plus a FAB for creation.
 * Top bar navigates to Stats and Sleep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    onOpenStats: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: (Long) -> Unit,
    viewModel: AlarmsViewModel = viewModel(
        factory = AlarmsViewModel.factory(
            (LocalContext.current.applicationContext as WakeProofApplication).appContainer.alarmRepository,
            (LocalContext.current.applicationContext as WakeProofApplication).appContainer.taskSessionRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.screen_alarms_title))
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stats),
                            contentDescription = stringResource(R.string.cd_open_stats)
                        )
                    }
                    IconButton(onClick = onOpenSleep) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sleep),
                            contentDescription = stringResource(R.string.cd_open_sleep)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.cd_open_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEditor(Destinations.NEW_ALARM_ID) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_alarm)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.alarms.isEmpty()) {
            EmptyAlarmsContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Index of the nearest ENABLED alarm: the streak banner
            // (spec 4.4) renders directly under its card.
            val nearestIndex = uiState.alarms.indexOfFirst { item ->
                item.alarm.isEnabled && item.minutesUntilTrigger != null
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uiState.alarms, key = { _, item -> item.alarm.id }) { index, item ->
                    Column {
                        AlarmCard(
                            item = item,
                            onOpen = { onOpenEditor(item.alarm.id) },
                            onToggle = { enabled -> viewModel.onToggleEnabled(item.alarm, enabled) }
                        )
                        if (index == nearestIndex) {
                            StreakBanner(streak = uiState.streak)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(
    item: AlarmListItem,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val alarm = item.alarm
    val contentAlpha = if (alarm.isEnabled) 1f else 0.45f

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
            }
            WeekdayRow(
                selected = alarm.daysOfWeek,
                modifier = Modifier.alpha(contentAlpha)
            )
            Text(
                text = nextTriggerLabel(alarm.isEnabled, item.minutesUntilTrigger),
                style = MaterialTheme.typography.bodyLarge,
                color = if (alarm.isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.alpha(contentAlpha)
            )
        }
    }
}

/** Compact weekday badges: selected days highlighted, others dimmed. */
@Composable
private fun WeekdayRow(
    selected: Set<DayOfWeek>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = day in selected
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = stringResource(weekdayShortLabelRes(day)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** "Fires in Xh Ym" label; [minutes] is null for disabled alarms. */
@Composable
private fun nextTriggerLabel(isEnabled: Boolean, minutes: Long?): String = when {
    !isEnabled -> stringResource(R.string.alarm_disabled)
    minutes == null -> stringResource(R.string.alarm_disabled)
    minutes >= 60 -> stringResource(R.string.alarm_next_in_h_m, minutes / 60, minutes % 60)
    else -> stringResource(R.string.alarm_next_in_m, minutes)
}

/** The streak badge under the nearest alarm (spec 4.4): «🔥 12». */
@Composable
private fun StreakBanner(streak: Int) {
    Text(
        text = stringResource(R.string.streak_badge, streak),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyAlarmsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.alarms_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.alarms_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
