package com.wakeproof.alarm.presentation.ui.ringing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.presentation.theme.WakeProofTheme
import com.wakeproof.alarm.service.RingingService
import com.wakeproof.alarm.service.RingingSession

/**
 * Full-screen ringing activity (spec 4.1.3, stage 2): launched by the
 * full-screen intent of the FGS notification, shown over the lock
 * screen with the screen turned on. Back does nothing, and leaving the
 * app makes the service bring the screen back within 2 seconds and
 * accelerate the volume ramp.
 *
 * Stage 4 replaces the temporary "Выключить" button with the task flow.
 */
class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Spec section 2: over the lock screen, screen on, never sleeps
        // while ringing (a wake alarm screen must not time out).
        // Activity-level API (27+) — the Window variants were removed
        // from newer SDKs.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = readAlarmId(intent)
        setContent {
            WakeProofTheme {
                val viewModel: RingingViewModel = viewModel(
                    factory = RingingViewModel.factory(
                        alarmId,
                        (application as WakeProofApplication).appContainer.alarmRepository
                    )
                )
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventsFlow.collect { event ->
                        if (event is RingingEvent.AlarmGone) {
                            dismiss(alarmId)
                        }
                    }
                }

                RingingScreen(
                    alarmTimeLabel = uiState.alarmTimeLabel,
                    onDismiss = { dismiss(alarmId) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        RingingSession.onUiShown()
    }

    override fun onStop() {
        super.onStop()
        RingingSession.onUiHidden()
    }

    /**
     * Temporary dismiss (stage 2): tell the service to stop ringing,
     * remove the notification and re-arm the alarm; then finish.
     */
    private fun dismiss(alarmId: Long) {
        startService(RingingService.dismissIntent(this, alarmId))
        finish()
    }

    private fun readAlarmId(intent: Intent?): Long =
        intent?.getLongExtra(RingingService.EXTRA_ALARM_ID, -1L) ?: -1L

    companion object {
        /** Intent used by the full-screen notification (and manual tests). */
        fun launchIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(RingingService.EXTRA_ALARM_ID, alarmId)
    }
}
