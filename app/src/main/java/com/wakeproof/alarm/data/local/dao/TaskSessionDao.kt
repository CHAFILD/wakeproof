package com.wakeproof.alarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wakeproof.alarm.data.local.entity.TaskSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO of the task_sessions table. Writes go through the session
 * recorder; the streak/statistics stages read snapshots and flows.
 */
@Dao
interface TaskSessionDao {

    @Insert
    suspend fun insert(session: TaskSessionEntity): Long

    @Query("SELECT * FROM ${TaskSessionEntity.TABLE_NAME} ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TaskSessionEntity>>

    /** Snapshot of the whole history, oldest first (streak calculations). */
    @Query("SELECT * FROM ${TaskSessionEntity.TABLE_NAME} ORDER BY startedAt ASC, id ASC")
    suspend fun getAllChronological(): List<TaskSessionEntity>

    @Query("SELECT * FROM ${TaskSessionEntity.TABLE_NAME} WHERE id = :id")
    suspend fun getById(id: Long): TaskSessionEntity?

    /** Row rewrite used by the snooze continuation (read-modify-write). */
    @Update
    suspend fun update(session: TaskSessionEntity)
}
