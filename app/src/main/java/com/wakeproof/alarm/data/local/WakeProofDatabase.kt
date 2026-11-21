package com.wakeproof.alarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wakeproof.alarm.data.local.dao.AlarmDao
import com.wakeproof.alarm.data.local.dao.EmergencyPhraseDao
import com.wakeproof.alarm.data.local.dao.TaskSessionDao
import com.wakeproof.alarm.data.local.entity.AlarmEntity
import com.wakeproof.alarm.data.local.entity.EmergencyPhraseEntity
import com.wakeproof.alarm.data.local.entity.TaskSessionEntity

/**
 * Room database for WakeProof.
 *
 * Version 1 was the stage-0 skeleton with only a placeholder table.
 * Version 2 adds the real alarms table (spec 4.1.1) and drops the
 * placeholder; see [MIGRATION_1_2].
 * Version 3 adds the task_sessions table (spec 4.1.4 / 6) and the
 * single-row emergency_phrase storage (spec 4.1.4); see
 * [MIGRATION_2_3].
 */
@Database(
    entities = [
        AlarmEntity::class,
        TaskSessionEntity::class,
        EmergencyPhraseEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WakeProofDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    abstract fun taskSessionDao(): TaskSessionDao

    abstract fun emergencyPhraseDao(): EmergencyPhraseDao

    companion object {
        const val NAME = "wakeproof.db"

        /**
         * 1 -> 2: drop the stage-0 placeholder table and create the alarms
         * table exactly as Room expects it (matches AlarmEntity).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS stage0_placeholder")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alarms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`hour` INTEGER NOT NULL, " +
                        "`minute` INTEGER NOT NULL, " +
                        "`daysOfWeekMask` INTEGER NOT NULL, " +
                        "`isEnabled` INTEGER NOT NULL, " +
                        "`taskTypesMask` INTEGER NOT NULL, " +
                        "`difficulty` TEXT NOT NULL, " +
                        "`snoozePolicy` TEXT NOT NULL, " +
                        "`isVolumeRampEnabled` INTEGER NOT NULL, " +
                        "`isRecheckEnabled` INTEGER NOT NULL, " +
                        "`recheckDelayMin` INTEGER NOT NULL, " +
                        "`taskCount` INTEGER NOT NULL, " +
                        "`anchorEnabled` INTEGER NOT NULL, " +
                        "`anchorId` TEXT, " +
                        "`isSleepReminderEnabled` INTEGER NOT NULL, " +
                        "`sleepTargetHours` INTEGER NOT NULL" +
                        ")"
                )
            }
        }

        /**
         * 2 -> 3: ringing session log (spec 4.1.4 / 6) and the local
         * emergency phrase storage (spec 4.1.4). Column shapes match the
         * entities exactly, including the alarmId index.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`alarmId` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, " +
                        "`finishedAt` INTEGER NOT NULL, " +
                        "`result` TEXT NOT NULL, " +
                        "`mistakes` INTEGER NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, " +
                        "`taskResults` TEXT NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_task_sessions_alarmId` " +
                        "ON `task_sessions` (`alarmId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `emergency_phrase` (" +
                        "`id` INTEGER PRIMARY KEY NOT NULL, " +
                        "`phrase` TEXT NOT NULL, " +
                        "`generatedAt` INTEGER NOT NULL" +
                        ")"
                )
            }
        }
    }
}

