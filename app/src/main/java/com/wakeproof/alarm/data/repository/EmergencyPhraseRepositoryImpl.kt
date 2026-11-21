package com.wakeproof.alarm.data.repository

import android.content.Context
import androidx.annotation.ArrayRes
import com.wakeproof.alarm.data.local.dao.EmergencyPhraseDao
import com.wakeproof.alarm.data.local.entity.EmergencyPhraseEntity
import com.wakeproof.alarm.domain.emergency.EmergencyPhraseGenerator
import com.wakeproof.alarm.domain.repository.EmergencyPhraseRepository
import com.wakeproof.alarm.domain.task.KotlinRandomSource

/**
 * Room-backed storage of the emergency exit phrase (spec 4.1.4). The
 * word dictionary lives in strings.xml and is read through Resources,
 * keeping the generator itself Android-free. Generation happens once:
 * [getOrCreatePhrase] inserts the row lazily on first use, and only an
 * explicit [regeneratePhrase] replaces it afterwards.
 */
class EmergencyPhraseRepositoryImpl(
    context: Context,
    @ArrayRes private val dictionaryRes: Int,
    private val dao: EmergencyPhraseDao,
    private val clock: () -> Long = System::currentTimeMillis
) : EmergencyPhraseRepository {

    private val appContext = context.applicationContext
    private val generator = EmergencyPhraseGenerator(KotlinRandomSource())

    override suspend fun getOrCreatePhrase(): String {
        dao.get()?.let { return it.phrase }
        return createAndStore()
    }

    override suspend fun regeneratePhrase(): String = createAndStore()

    private suspend fun createAndStore(): String {
        val dictionary = appContext.resources.getStringArray(dictionaryRes).toList()
        val phrase = generator.generate(dictionary)
        dao.upsert(
            EmergencyPhraseEntity(
                phrase = phrase,
                generatedAt = clock()
            )
        )
        return phrase
    }
}
