package com.wakeproof.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wakeproof.alarm.presentation.navigation.WakeProofApp
import com.wakeproof.alarm.presentation.theme.WakeProofTheme

/**
 * Single Compose activity hosting the NavHost graph. The ringing
 * screen will be a dedicated activity from Stage 2.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeProofTheme {
                WakeProofApp()
            }
        }
    }
}
