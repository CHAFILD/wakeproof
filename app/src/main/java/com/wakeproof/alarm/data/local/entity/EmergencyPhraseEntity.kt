package com.wakeproof.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row local storage of the emergency exit phrase, spec 4.1.4:
 * generated once at first use, replaced only by an explicit
 * regeneration in Settings. Room is used instead of adding a new
 * DataStore dependency (no new libraries allowed at this stage).
 */
@Entity(tableName = EmergencyPhraseEntity.TABLE_NAME)
data class EmergencyPhraseEntity(
    /** Always [SINGLE_ROW_ID] - there is exactly one phrase. */
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val phrase: String,
    /** Epoch ms of generation, informational. */
    val generatedAt: Long
) {
    companion object {
        const val TABLE_NAME = "emergency_phrase"
        const val SINGLE_ROW_ID = 1
    }
}
