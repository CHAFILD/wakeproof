package com.wakeproof.alarm.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

/**
 * Temporary entity: Room cannot process a database with an empty entity
 * list, so Stage 0 ships this placeholder. Stage 1 adds the real
 * AlarmEntity together with a migration; the placeholder table can be
 * dropped in that migration.
 */
@Entity(tableName = "stage0_placeholder")
data class Stage0PlaceholderEntity(
    @PrimaryKey val id: Long = 0
)

/**
 * Room database for WakeProof.
 *
 * Version 1 has no real entities yet: alarms, sessions, scores and
 * recordings arrive in later stages together with migrations.
 */
@Database(
    entities = [Stage0PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WakeProofDatabase : RoomDatabase() {

    companion object {
        const val NAME = "wakeproof.db"
    }
}
