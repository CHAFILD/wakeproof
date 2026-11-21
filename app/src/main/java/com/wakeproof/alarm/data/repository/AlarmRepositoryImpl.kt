package com.wakeproof.alarm.data.repository

import com.wakeproof.alarm.data.local.dao.AlarmDao
import com.wakeproof.alarm.data.mapper.AlarmMapper
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [AlarmRepository]. Mapping to the domain
 * model happens here so the UI never touches entities.
 */
class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun observeAlarms(): Flow<List<Alarm>> =
        alarmDao.observeAll().map { entities -> entities.map(AlarmMapper::toDomain) }

    override suspend fun getAlarmById(id: Long): Alarm? =
        alarmDao.getById(id)?.let(AlarmMapper::toDomain)

    override suspend fun upsert(alarm: Alarm): Long =
        alarmDao.upsert(AlarmMapper.toEntity(alarm))

    override suspend fun deleteAlarmById(id: Long) =
        alarmDao.deleteById(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) =
        alarmDao.setEnabled(id, enabled)
}
