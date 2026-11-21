package com.wakeproof.alarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wakeproof.alarm.data.local.entity.EmergencyPhraseEntity

/** DAO of the single-row emergency phrase storage (spec 4.1.4). */
@Dao
interface EmergencyPhraseDao {

    @Query("SELECT * FROM ${EmergencyPhraseEntity.TABLE_NAME} WHERE id = ${EmergencyPhraseEntity.SINGLE_ROW_ID}")
    suspend fun get(): EmergencyPhraseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmergencyPhraseEntity)
}
