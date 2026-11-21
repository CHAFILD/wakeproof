package com.wakeproof.alarm.domain.emergency

/**
 * Compares the typed emergency phrase with the stored one, spec 4.1.4:
 * "uncomfortable but never a dead end". The comparison is deliberately
 * forgiving about case, repeated spaces and the ё/е keyboard ambiguity
 * (many Russian keyboards make typing ѐ awkward, and autocorrect often
 * swaps ё for е) while still requiring the exact four words in order.
 */
object EmergencyPhraseMatcher {

    fun matches(input: String, phrase: String): Boolean =
        normalize(input) == normalize(phrase)

    /**
     * trim + lowercase + collapse inner whitespace + ё→е.
     */
    fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace('ё', 'е')
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
