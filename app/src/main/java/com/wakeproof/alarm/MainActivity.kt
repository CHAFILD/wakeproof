package com.wakeproof.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.wakeproof.alarm.presentation.navigation.WakeProofApp
import com.wakeproof.alarm.presentation.theme.WakeProofTheme

/**
 * Single Compose activity hosting the NavHost graph. The ringing
 * screen is a dedicated full-screen activity (stage 2).
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not: ringing works regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            WakeProofTheme {
                WakeProofApp()
            }
        }
    }

    /**
     * Spec section 3: POST_NOTIFICATIONS is requested on first launch
     * (Android 13+). Without it the ringing FGS notification stays
     * hidden — the alarm still rings and the full-screen intent still
     * launches, but the collapsed notification is not shown.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(permission)
        }
    }
}
