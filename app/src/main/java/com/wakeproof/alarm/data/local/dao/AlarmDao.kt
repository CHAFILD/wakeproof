package com.wakeproof.alarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wakeproof.alarm.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

/**
 * Alarm DAO. The list query returns rows ordered by ring time so the UI
 * shows alarms chronologically.
 */
@Dao
interface AlarmDao {

    @Query("SELECT * FROM ${AlarmEntity.TABLE_NAME} ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM ${AlarmEntity.TABLE_NAME} WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity): Long

    @Query("DELETE FROM ${AlarmEntity.TABLE_NAME} WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ${AlarmEntity.TABLE_NAME} SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
