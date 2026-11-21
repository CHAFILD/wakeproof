package com.wakeproof.alarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wakeproof.alarm.data.local.dao.AlarmDao
import com.wakeproof.alarm.data.local.entity.AlarmEntity

/**
 * Room database for WakeProof.
 *
 * Version 1 was the stage-0 skeleton with only a placeholder table.
 * Version 2 adds the real alarms table (spec 4.1.1) and drops the
 * placeholder; see [MIGRATION_1_2].
 */
@Database(
    entities = [AlarmEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WakeProofDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

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
    }
}
