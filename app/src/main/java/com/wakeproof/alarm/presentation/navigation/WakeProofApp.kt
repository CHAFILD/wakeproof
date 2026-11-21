package com.wakeproof.alarm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakeproof.alarm.presentation.ui.alarms.AlarmsScreen
import com.wakeproof.alarm.presentation.ui.editor.AlarmEditorScreen
import com.wakeproof.alarm.presentation.ui.settings.SettingsScreen
import com.wakeproof.alarm.presentation.ui.sleep.SleepScreen
import com.wakeproof.alarm.presentation.ui.stats.StatsScreen

/**
 * Root composable of the app UI: dark theme + flat NavHost with the alarms
 * list, the alarm editor and the stub screens. The ringing screen is a
 * separate full-screen activity starting from Stage 2, so it is not part
 * of this graph.
 */
@Composable
fun WakeProofApp(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destinations.ALARMS,
        modifier = modifier
    ) {
        composable(Destinations.ALARMS) {
            AlarmsScreen(
                onOpenStats = { navController.navigate(Destinations.STATS) },
                onOpenSleep = { navController.navigate(Destinations.SLEEP) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onOpenEditor = { alarmId ->
                    navController.navigate(Destinations.alarmEditor(alarmId))
                }
            )
        }
        composable(
            route = Destinations.ALARM_EDITOR,
            arguments = listOf(
                navArgument(Destinations.ARG_ALARM_ID) {
                    type = NavType.LongType
                    defaultValue = Destinations.NEW_ALARM_ID
                }
            )
        ) {
            AlarmEditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.STATS) {
            StatsScreen()
        }
        composable(Destinations.SLEEP) {
            SleepScreen()
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
