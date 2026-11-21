package com.wakeproof.alarm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.wakeproof.alarm.R
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.domain.audio.VolumeRamp
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.presentation.ui.ringing.RingingActivity
import com.wakeproof.alarm.util.AlarmNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service of type mediaPlayback that rings the alarm
 * (spec 4.1.3 / 4.1.5 and stage 2 plan):
 *
 * - plays the raw alarm sound through AudioAttributes USAGE_ALARM
 *   (audible in Do Not Disturb with the standard "Alarms" allowance);
 * - ramps the volume 20% -> 100% linearly, +5% every 3 s, with the
 *   vibration waveform re-issued on every step (synchronous ramp);
 * - accelerates the ramp and re-fires the full-screen intent when the
 *   ringing UI has been invisible for 2 seconds (user swiped away /
 *   pressed home / power);
 * - on dismissal stops sound and vibration, removes the notification
 *   and re-arms the alarm for its next occurrence.
 */
class RingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var rampJob: Job? = null
    private var uiWatchJob: Job? = null

    /** Id of the alarm currently ringing; -1 when idle. */
    @Volatile
    private var alarmId: Long = -1L

    @Volatile
    private var currentVolume: Float = VolumeRamp.START_VOLUME

    @Volatile
    private var isRampAccelerated: Boolean = false

    @Volatile
    private var isRinging: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                val id = intent.getLongExtra(EXTRA_ALARM_ID, alarmId)
                dismiss(id)
            }
            else -> {
                val id = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
                if (id <= 0L) {
                    Log.w(TAG, "Start without alarm id, stopping")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRinging(id)
            }
        }
        // Not sticky: if the system kills the ringing service the alarm
        // schedule for the next occurrence is already armed by AlarmReceiver.
        return START_NOT_STICKY
    }

    // --- Ringing lifecycle -------------------------------------------------

    private fun startRinging(id: Long) {
        if (isRinging && id == alarmId) return // already ringing this alarm
        stopPlayback()

        alarmId = id
        isRinging = true
        currentVolume = VolumeRamp.START_VOLUME
        isRampAccelerated = false

        // FGS notification first (startForeground must happen right away),
        // it also carries the full-screen intent for the lock screen.
        val notification = AlarmNotifications.buildRingingNotification(this, id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, AlarmNotifications.RINGING_NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            ServiceCompat.startForeground(
                this, AlarmNotifications.RINGING_NOTIFICATION_ID, notification, 0
            )
        }
        acquireWakeLock()

        uiWatchJob?.cancel()
        uiWatchJob = serviceScope.launch { watchRingingUi() }

        serviceScope.launch {
            val alarm = withContext(Dispatchers.IO) {
                container().alarmRepository.getAlarmById(id)
            }
            if (alarm == null || !alarm.isEnabled) {
                // Alarm was deleted or disabled while the pending intent flew.
                Log.w(TAG, "Alarm id=$id vanished before ringing, dismissing")
                dismiss(id)
            } else {
                if (!alarm.isVolumeRampEnabled || alarm.difficulty == Difficulty.HARDCORE) {
                    // Spec 4.1.5: no ramp -> 100% from the first second.
                    // Spec 4.2.1: HARDCORE always rings at full volume.
                    currentVolume = VolumeRamp.MAX_VOLUME
                }
                startPlayback()
            }
        }
    }

    /**
     * Dismiss flow (temporary button on the ringing screen, stage 4
     * replaces it with tasks): stop everything, remove the notification
     * and re-arm the alarm for its next occurrence (spec item 6).
     */
    private fun dismiss(id: Long) {
        val known = isRinging
        isRinging = false
        uiWatchJob?.cancel()
        stopPlayback()
        AlarmNotifications.cancelRinging(this)
        releaseWakeLock()
        if (known) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()

        if (id <= 0L) return // defensive: nothing to re-arm

        // Reschedule on the application scope so the coroutine survives
        // this service instance being destroyed immediately afterwards.
        container().applicationScope.launch {
            val alarm = container().alarmRepository.getAlarmById(id)
            if (alarm != null && alarm.isEnabled) {
                container().alarmScheduler.schedule(alarm)
            } else {
                container().alarmScheduler.cancel(id)
            }
        }
    }

    override fun onDestroy() {
        // Safety net for system-initiated destruction.
        stopPlayback()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- Audio + vibration -------------------------------------------------

    private fun startPlayback() {
        startMediaPlayer()
        startVibrator()
        restartRampLoop()
    }

    private fun stopPlayback() {
        rampJob?.cancel()
        rampJob = null
        stopMediaPlayer()
        stopVibrator()
    }

    private fun startMediaPlayer() {
        stopMediaPlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            resources.openRawResourceFd(R.raw.alarm_ring)?.use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.isLooping = true
            player.setOnPreparedListener { mp ->
                if (isRinging) {
                    mp.setVolume(currentVolume, currentVolume)
                    mp.start()
                } else {
                    mp.release()
                }
            }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
                true
            }
            player.prepareAsync()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to init MediaPlayer", t)
            player.release()
            mediaPlayer = null
        }
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.run {
            runCatching { stop() }
            release()
        }
        mediaPlayer = null
    }

    /** Re-issues the repeating waveform with amplitude scaled to [volume]. */
    private fun applyVolumeStep(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
        val vib = vibrator ?: return
        val amplitude = (volume * 255).toInt().coerceIn(1, 255)
        val timings = longArrayOf(0, 700, 350, 700, 1400)
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vib.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(
                timings, intArrayOf(0, amplitude, 0, amplitude, 0), 0
            )
        } else {
            VibrationEffect.createWaveform(timings, 0)
        }
        vib.cancel()
        vib.vibrate(effect)
    }

    /**
     * The ramp loop (spec 4.1.5): apply, wait one step interval, grow.
     * Restarting the loop switches the cadence when the ramp accelerates.
     */
    private fun restartRampLoop() {
        rampJob?.cancel()
        rampJob = serviceScope.launch {
            applyVolumeStep(currentVolume)
            while (isActive && isRinging && currentVolume < VolumeRamp.MAX_VOLUME) {
                delay(VolumeRamp.intervalMs(isRampAccelerated))
                currentVolume = VolumeRamp.nextVolume(currentVolume, isRampAccelerated)
                applyVolumeStep(currentVolume)
            }
        }
    }

    private fun startVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun stopVibrator() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "wakeproof:ringing"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // --- Bringing the UI back (spec 4.1.3) ---------------------------------

    /**
     * While ringing: when the ringing screen has been invisible for
     * [UI_RETURN_GRACE_MS], accelerate the ramp (never above 100%,
     * spec 4.1.5) and re-fire the full-screen intent. collectLatest
     * cancels the grace countdown on every quick visibility bounce
     * (rotation), so only real departures trigger it.
     */
    private suspend fun watchRingingUi() {
        RingingSession.isRingingUiVisible.collectLatest { visible ->
            if (!visible && isRinging) {
                delay(UI_RETURN_GRACE_MS)
                if (!RingingSession.isRingingUiVisible.value && isRinging) {
                    Log.i(TAG, "Ringing UI lost, accelerating ramp and re-firing FSI")
                    accelerateRamp()
                    // Screen off / locked: the system launches the
                    // full-screen intent by itself.
                    AlarmNotifications.repostRinging(this@RingingService, alarmId)
                    // Screen on and unlocked: the platform downgrades the
                    // full-screen intent to a heads-up banner, so also try
                    // bringing the activity forward directly. Denials are
                    // expected on some versions and are ignored.
                    bringRingingUiToFront()
                }
            }
        }
    }

    private fun bringRingingUiToFront() {
        try {
            startActivity(RingingActivity.launchIntent(this, alarmId))
        } catch (t: Throwable) {
            Log.w(TAG, "Direct ringing UI launch not allowed: ${t.message}")
        }
    }

    /** Switches the ramp to the fast curve, continuing from the current volume. */
    private fun accelerateRamp() {
        if (currentVolume >= VolumeRamp.MAX_VOLUME) return
        if (!isRampAccelerated) {
            isRampAccelerated = true
            restartRampLoop()
        }
    }

    private fun container() = (application as WakeProofApplication).appContainer

    companion object {
        private const val TAG = "RingingService"

        /** Spec 4.1.3: the screen must come back within 2 seconds. */
        private const val UI_RETURN_GRACE_MS = 2_000L

        /** Safety ceiling: 30 minutes of ringing is beyond any session. */
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L

        const val ACTION_START = "com.wakeproof.alarm.action.START_RINGING"
        const val ACTION_DISMISS = "com.wakeproof.alarm.action.DISMISS_RINGING"
        const val EXTRA_ALARM_ID = "com.wakeproof.alarm.extra.ALARM_ID"

        /** Intent that (re)starts ringing for [alarmId]. */
        fun startIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_ID, alarmId)

        /** Intent that stops ringing and re-arms the alarm. */
        fun dismissIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingingService::class.java)
                .setAction(ACTION_DISMISS)
                .putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
