package com.wakeproof.alarm.domain.repository

/**
 * Storage of the emergency exit phrase, spec 4.1.4: generated ONCE at
 * first use, stored locally, viewable and regenerable from Settings →
 * "Аварийный выход". The phrase never leaves the device and is never
 * logged.
 */
interface EmergencyPhraseRepository {

    /**
     * Returns the stored phrase, generating and persisting it on the
     * very first call (first app use).
     */
    suspend fun getOrCreatePhrase(): String

    /** Replaces the stored phrase with a freshly generated one. */
    suspend fun regeneratePhrase(): String
}
