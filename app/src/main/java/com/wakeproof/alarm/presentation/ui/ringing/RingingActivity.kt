package com.wakeproof.alarm.presentation.ui.ringing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.presentation.theme.WakeProofTheme
import com.wakeproof.alarm.service.RingingService
import com.wakeproof.alarm.service.RingingSession

/**
 * Full-screen ringing activity (spec 4.1.3): launched by the
 * full-screen intent of the FGS notification, shown over the lock
 * screen with the screen turned on. Back does nothing, and leaving
 * the app makes the service bring the screen back within 2 seconds
 * and accelerate the volume ramp (stage 2).
 *
 * Stage 4: the alarm is dismissed ONLY by solving the task queue or
 * through the emergency exit; the summary and emergency-done screens
 * stay on screen after the sound has stopped.
 */
class RingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Spec section 2: over the lock screen, screen on, never sleeps
        // while ringing (a wake alarm screen must not time out).
        // Activity-level API (27+) — the Window variants were removed
        // from newer SDKs.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmId = readAlarmId(intent)
        setContent {
            WakeProofTheme {
                val container = (application as WakeProofApplication).appContainer
                val viewModel: RingingViewModel = viewModel(
                    factory = RingingViewModel.factory(
                        alarmId,
                        container.alarmRepository,
                        container.taskSessionRepository,
                        container.emergencyPhraseRepository
                    )
                )

                LaunchedEffect(Unit) {
                    viewModel.eventsFlow.collect { event ->
                        when (event) {
                            RingingEvent.AlarmGone -> {
                                // Alarm deleted mid-ring (or no id):
                                // stop everything and close.
                                startService(RingingService.dismissIntent(this@RingingActivity, alarmId))
                                finish()
                            }
                            RingingEvent.StopRinging -> {
                                // Tasks solved / emergency exit: stop the
                                // sound + notification and re-arm the alarm,
                                // but keep the UI (summary) on screen.
                                startService(RingingService.dismissIntent(this@RingingActivity, alarmId))
                            }
                            RingingEvent.SnoozeArmed -> {
                                // Snooze (spec 4.1.2): the alarm row now
                                // carries the +10 min target, so the dismiss
                                // re-arm schedules the snoozed ring; close
                                // the ringing UI right away.
                                startService(RingingService.dismissIntent(this@RingingActivity, alarmId))
                                finish()
                            }
                        }
                    }
                }

                RingingScreen(
                    viewModel = viewModel,
                    onCloseUi = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmId = readAlarmId(intent)
    }

    override fun onStart() {
        super.onStart()
        RingingSession.onUiShown()
    }

    override fun onStop() {
        super.onStop()
        RingingSession.onUiHidden()
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
