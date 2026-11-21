package com.wakeproof.alarm.domain.emergency

import com.wakeproof.alarm.domain.task.RandomSource

/**
 * Generates the emergency exit phrase, spec 4.1.4: 4 distinct words
 * drawn from the local ~200-word Russian dictionary. The dictionary
 * itself lives in resources (strings.xml) and is passed in, keeping the
 * domain layer free of Android.
 */
class EmergencyPhraseGenerator(
    private val random: RandomSource
) {

    /**
     * @param dictionary local word list; must contain at least
     *   [WORD_COUNT] entries.
     * @return the phrase as four words separated by single spaces.
     */
    fun generate(dictionary: List<String>): String {
        require(dictionary.size >= WORD_COUNT) {
            "Emergency dictionary must contain at least $WORD_COUNT words."
        }
        val used = mutableSetOf<Int>()
        val words = mutableListOf<String>()
        while (words.size < WORD_COUNT) {
            val index = random.nextInt(0, dictionary.size)
            // Distinct words within one phrase (no "сосна сосна").
            if (used.add(index)) {
                words += dictionary[index]
            }
        }
        return words.joinToString(separator = SEPARATOR)
    }

    companion object {
        /** Spec 4.1.4: the phrase consists of 4 words. */
        const val WORD_COUNT = 4
        const val SEPARATOR = " "
    }
}
