package com.wakeproof.alarm.data.mapper

import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.task.TaskResult

/**
 * Encodes the per-task results of a session into one Room column and
 * back. Format: "TYPE|timeTakenMs|mistakes" joined with ';' - enum
 * names contain no separators, so the encoding is unambiguous. Plain
 * string building keeps the stage free of new serialization
 * dependencies.
 */
object TaskResultsCodec {

    private const val FIELD_SEPARATOR = "|"
    private const val ITEM_SEPARATOR = ";"

    fun encode(results: List<TaskResult>): String =
        results.joinToString(ITEM_SEPARATOR) { result ->
            result.taskType.name + FIELD_SEPARATOR +
                result.timeTakenMs + FIELD_SEPARATOR +
                result.mistakes
        }

    /**
     * Decodes a stored column. Unknown type names (from a future task
     * type) are skipped defensively instead of crashing the reader.
     */
    fun decode(value: String): List<TaskResult> =
        if (value.isBlank()) {
            emptyList()
        } else {
            value.split(ITEM_SEPARATOR)
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    val parts = item.split(FIELD_SEPARATOR)
                    if (parts.size != 3) return@mapNotNull null
                    val type = runCatching { TaskType.valueOf(parts[0]) }
                        .getOrNull() ?: return@mapNotNull null
                    val time = parts[1].toLongOrNull() ?: return@mapNotNull null
                    val mistakes = parts[2].toIntOrNull() ?: return@mapNotNull null
                    TaskResult(
                        timeTakenMs = time,
                        mistakes = mistakes,
                        taskType = type
                    )
                }
        }
}
