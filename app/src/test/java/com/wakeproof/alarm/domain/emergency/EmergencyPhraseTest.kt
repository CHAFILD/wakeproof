package com.wakeproof.alarm.domain.emergency

import com.wakeproof.alarm.domain.task.KotlinRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Emergency phrase (spec 4.1.4): 4 distinct dictionary words, stable
 * under a fixed seed, and the matcher tolerates case/space/ё-padding
 * but not wrong words.
 */
class EmergencyPhraseTest {

    private val dictionary = listOf(
        "арбуз", "банан", "ветер", "гроза", "дом", "ежевика", "жук", "зима",
        "икра", "камень", "лето", "море"
    )

    @Test
    fun `phrase consists of exactly four dictionary words`() {
        val phrase = EmergencyPhraseGenerator(KotlinRandomSource.seeded(1L))
            .generate(dictionary)
        val words = phrase.split(" ")
        assertEquals(EmergencyPhraseGenerator.WORD_COUNT, words.size)
        assertTrue(words.all { it in dictionary })
    }

    @Test
    fun `words inside one phrase are distinct`() {
        for (seed in 1..50L) {
            val words = EmergencyPhraseGenerator(KotlinRandomSource.seeded(seed))
                .generate(dictionary)
                .split(" ")
            assertEquals(words.size, words.toSet().size, "Duplicate words for seed=$seed")
        }
    }

    @Test
    fun `generation is deterministic under a fixed seed`() {
        val generator = { EmergencyPhraseGenerator(KotlinRandomSource.seeded(99L)).generate(dictionary) }
        assertEquals(generator(), generator())
    }

    @Test
    fun `different seeds usually give different phrases`() {
        val first = EmergencyPhraseGenerator(KotlinRandomSource.seeded(1L)).generate(dictionary)
        var anyDifferent = false
        for (seed in 2..30L) {
            if (EmergencyPhraseGenerator(KotlinRandomSource.seeded(seed)).generate(dictionary) != first) {
                anyDifferent = true
                break
            }
        }
        assertTrue(anyDifferent, "At least one of 30 seeds must produce another phrase")
    }

    @Test
    fun `too small dictionary is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            EmergencyPhraseGenerator(KotlinRandomSource.seeded(1L))
                .generate(listOf("раз", "два"))
        }
    }

    // --- Matcher ------------------------------------------------------------

    private val phrase = "арбуз банан ветер гроза"

    @Test
    fun `matcher accepts the exact phrase`() {
        assertTrue(EmergencyPhraseMatcher.matches(phrase, phrase))
    }

    @Test
    fun `matcher ignores case`() {
        assertTrue(EmergencyPhraseMatcher.matches("Арбуз БАНАН ветер грОЗа", phrase))
    }

    @Test
    fun `matcher collapses extra spaces and trims`() {
        assertTrue(EmergencyPhraseMatcher.matches("  арбуз   банан  ветер   гроза  ", phrase))
    }

    @Test
    fun `matcher treats ё and е as the same letter`() {
        assertTrue(EmergencyPhraseMatcher.matches("ёжик", "ежик"))
    }

    @Test
    fun `matcher rejects wrong words or order`() {
        assertFalse(EmergencyPhraseMatcher.matches("банан арбуз ветер гроза", phrase))
        assertFalse(EmergencyPhraseMatcher.matches("арбуз банан ветер", phrase))
        assertFalse(EmergencyPhraseMatcher.matches("арбуз банан ветер гроза дом", phrase))
        assertFalse(EmergencyPhraseMatcher.matches("арбуз банан ветер гриба", phrase))
    }

    @Test
    fun `normalize drops lone words edge cases`() {
        assertEquals("а б", EmergencyPhraseMatcher.normalize("  А  б "))
        assertEquals("", EmergencyPhraseMatcher.normalize("   "))
    }
}
