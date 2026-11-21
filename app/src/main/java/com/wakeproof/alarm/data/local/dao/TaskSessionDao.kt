package com.wakeproof.alarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wakeproof.alarm.data.local.entity.TaskSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO of the task_sessions table. This stage only inserts; the
 * statistics stage will query aggregates.
 */
@Dao
interface TaskSessionDao {

    @Insert
    suspend fun insert(session: TaskSessionEntity): Long

    @Query("SELECT * FROM ${TaskSessionEntity.TABLE_NAME} ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TaskSessionEntity>>
}
