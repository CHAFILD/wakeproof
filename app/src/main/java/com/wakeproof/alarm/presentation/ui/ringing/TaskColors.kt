package com.wakeproof.alarm.presentation.ui.ringing

import androidx.compose.ui.graphics.Color
import com.wakeproof.alarm.R
import com.wakeproof.alarm.domain.task.TaskColor

/**
 * UI mapping of the four task colors (spec 4.2.1): bright fills for
 * the dark ringing screen plus the Russian label resources for the
 * Stroop buttons and content descriptions.
 */

/** Bright fill of a task color button on the dark background. */
val TaskColor.fill: Color
    get() = when (this) {
        TaskColor.RED -> Color(0xFFE53935)
        TaskColor.GREEN -> Color(0xFF43A047)
        TaskColor.BLUE -> Color(0xFF42A5F5)
        TaskColor.YELLOW -> Color(0xFFFDD835)
    }

/** Even brighter variant for the flash highlight. */
val TaskColor.flashFill: Color
    get() = when (this) {
        TaskColor.RED -> Color(0xFFFF6F60)
        TaskColor.GREEN -> Color(0xFF7CE87C)
        TaskColor.BLUE -> Color(0xFF8AC4FF)
        TaskColor.YELLOW -> Color(0xFFFFF176)
    }

/** Russian name of the color (КРАСНЫЙ, ЗЕЛЁНЫЙ, СИНИЙ, ЖЁЛТЫЙ). */
val TaskColor.labelRes: Int
    get() = when (this) {
        TaskColor.RED -> R.string.task_color_red
        TaskColor.GREEN -> R.string.task_color_green
        TaskColor.BLUE -> R.string.task_color_blue
        TaskColor.YELLOW -> R.string.task_color_yellow
    }

/** Fixed visual order of the four buttons in the 2x2 grids. */
val TASK_GRID_COLORS: List<TaskColor> = listOf(
    TaskColor.RED,
    TaskColor.GREEN,
    TaskColor.BLUE,
    TaskColor.YELLOW
)
