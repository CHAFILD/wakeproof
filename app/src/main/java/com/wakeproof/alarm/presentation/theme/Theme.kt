package com.wakeproof.alarm.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app is always dark: a wake-up app must never flash a bright screen
 * at night, so the light scheme is intentionally not provided.
 */
private val WakeProofDarkColors = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary = SandSecondary,
    onSecondary = SandOnSecondary,
    secondaryContainer = SandContainer,
    onSecondaryContainer = SandOnContainer,
    tertiary = MistTertiary,
    onTertiary = MistOnTertiary,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    error = ErrorRed,
    onError = OnErrorRed
)

@Composable
fun WakeProofTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WakeProofDarkColors,
        typography = WakeProofTypography,
        content = content
    )
}
